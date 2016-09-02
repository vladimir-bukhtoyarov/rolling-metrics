/*
 *
 *  Copyright 2016 Vladimir Bukhtoyarov
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.github.metricscore.hdr.histogram.accumulator;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Snapshot;
import com.github.metricscore.hdr.histogram.util.EmptySnapshot;
import com.github.metricscore.hdr.histogram.util.Printer;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Function;
import java.util.function.Supplier;

public class ResetByChunksAccumulator implements Accumulator {

    private static final long FAKE_VALUE = Long.MIN_VALUE;

    private final long intervalBetweenResettingMillis;
    private final long creationTimestamp;
    private final boolean reportUncompletedChunkToSnapshot;
    private final Chunk[] chunks;
    private final Clock clock;
    private final Histogram temporarySnapshotHistogram;

    private final Phase left;
    private final Phase right;
    private final Phase[] phases;
    private final AtomicReference<Phase> currentPhaseRef;
    private final AtomicInteger activeMutators = new AtomicInteger(0);

    private volatile Runnable postponedPhaseRotation = null;

    public ResetByChunksAccumulator(Supplier<Recorder> recorderSupplier, int numberChunks, long intervalBetweenResettingMillis, boolean reportUncompletedChunkToSnapshot, Clock clock) {
        this.intervalBetweenResettingMillis = intervalBetweenResettingMillis;
        this.clock = clock;
        this.creationTimestamp = clock.getTime();
        this.reportUncompletedChunkToSnapshot = reportUncompletedChunkToSnapshot;

        this.left = new Phase(recorderSupplier, creationTimestamp + intervalBetweenResettingMillis, reportUncompletedChunkToSnapshot);
        this.right = new Phase(recorderSupplier, Long.MAX_VALUE, reportUncompletedChunkToSnapshot);
        this.phases = new Phase[] {left, right};
        this.currentPhaseRef = new AtomicReference<>(left);

        this.chunks = new Chunk[numberChunks];
        for (int i = 0; i < numberChunks; i++) {
            Histogram chunkHistogram = left.intervalHistogram.copy();
            this.chunks[i] = new Chunk(chunkHistogram, Long.MIN_VALUE);
        }
        this.temporarySnapshotHistogram = chunks[0].histogram.copy();
    }

    @Override
    public void recordSingleValueWithExpectedInterval(long value, long expectedIntervalBetweenValueSamples) {
        long currentTimeMillis = clock.getTime();
        recordOrTouch(value, expectedIntervalBetweenValueSamples, currentTimeMillis, true);
    }

    @Override
    public final synchronized Snapshot getSnapshot(Function<Histogram, Snapshot> snapshotTaker) {
        temporarySnapshotHistogram.reset();
        long currentTimeMillis = clock.getTime();
        while (!activeMutators.compareAndSet(0, 1)) {
            // if phase rotation process is in progress by writer thread then wait inside spin loop until rotation will be done
            LockSupport.parkNanos(TimeUnit.MICROSECONDS.toNanos(500));
            if (Thread.currentThread().isInterrupted()) {
                return EmptySnapshot.INSTANCE;
            }
        }

        try {
            for (Phase phase : phases) {
                if (phase.proposedInvalidationTimestamp <= currentTimeMillis) {
                    // detected that reservoir was unused by writer for a long time
                    // lets record fake value in order to trigger the rotation process
                    recordOrTouch(FAKE_VALUE, 0, currentTimeMillis, false);
                }
                if (reportUncompletedChunkToSnapshot) {
                    phase.intervalHistogram = phase.recorder.getIntervalHistogram(phase.intervalHistogram);
                    phase.totalsHistogram.add(phase.intervalHistogram);
                    temporarySnapshotHistogram.add(phase.totalsHistogram);
                }
            }
            for (Chunk chunk : chunks) {
                if (chunk.proposedInvalidationTimestamp > currentTimeMillis) {
                    temporarySnapshotHistogram.add(chunk.histogram);
                }
            }
        } finally {
            if (activeMutators.decrementAndGet() > 0) {
                while (this.postponedPhaseRotation == null) {
                    // wait in spin loop until writer thread provide rotation task
                    LockSupport.parkNanos(TimeUnit.MICROSECONDS.toNanos(100));
                }
                postponedPhaseRotation.run();
            }
        }
        return snapshotTaker.apply(temporarySnapshotHistogram);
    }

    private void recordOrTouch(long value, long expectedIntervalBetweenValueSamples, long currentTimeMillis, boolean currentThreadIsWriter) {
        Phase currentPhase = currentPhaseRef.get();
        if (currentTimeMillis < currentPhase.proposedInvalidationTimestamp) {
            if (currentThreadIsWriter) {
                currentPhase.recorder.recordValueWithExpectedInterval(value, expectedIntervalBetweenValueSamples);
            }
            return;
        }

        Phase nextPhase = currentPhase == left ? right : left;
        if (currentThreadIsWriter) {
            nextPhase.recorder.recordValueWithExpectedInterval(value, expectedIntervalBetweenValueSamples);
        }
        if (!currentPhaseRef.compareAndSet(currentPhase, nextPhase)) {
            // another writer achieved progress and must clear current phase data, current writer tread just can write value to next phase and return
            return;
        }

        // Current thread is responsible to rotate phases.
        Runnable phaseRotation = () -> {
            try {
                postponedPhaseRotation = null;

                // move values from recorder to correspondent chunk
                long currentPhaseNumber = (currentPhase.proposedInvalidationTimestamp - creationTimestamp) / intervalBetweenResettingMillis;
                int correspondentChunkIndex = (int) (currentPhaseNumber - 1) % chunks.length;
                currentPhase.intervalHistogram = currentPhase.recorder.getIntervalHistogram(currentPhase.intervalHistogram);
                Chunk correspondentChunk = chunks[correspondentChunkIndex];
                correspondentChunk.histogram.reset();
                if (reportUncompletedChunkToSnapshot) {
                    currentPhase.totalsHistogram.add(currentPhase.intervalHistogram);
                    correspondentChunk.histogram.add(currentPhase.totalsHistogram);
                    currentPhase.totalsHistogram.reset();
                } else {
                    correspondentChunk.histogram.add(currentPhase.intervalHistogram);
                }
                correspondentChunk.proposedInvalidationTimestamp = currentPhase.proposedInvalidationTimestamp + (chunks.length - 1) * intervalBetweenResettingMillis;
            } finally {
                long millisSinceCreation = currentTimeMillis - creationTimestamp;
                long intervalsSinceCreation = millisSinceCreation / intervalBetweenResettingMillis;
                currentPhase.proposedInvalidationTimestamp = Long.MAX_VALUE;
                activeMutators.decrementAndGet();
                nextPhase.proposedInvalidationTimestamp = creationTimestamp + (intervalsSinceCreation + 1) * intervalBetweenResettingMillis;
            }
        };

        // Need to be aware about snapshot takers in the middle of progress state
        if (activeMutators.incrementAndGet() > 1 && currentThreadIsWriter) {
            // give chance to snapshot taker to finalize snapshot extraction, rotation will be complete by snapshot taker thread
            postponedPhaseRotation = phaseRotation;
        } else {
            // There are no active snapshot takers in the progress state, lets exchange phases in this writer thread
            phaseRotation.run();
        }
    }

    @Override
    public int getEstimatedFootprintInBytes() {
        // each histogram has equivalent pessimistic estimation
        int oneHistogramPessimisticFootprint = temporarySnapshotHistogram.getEstimatedFootprintInBytes();

        // 4 - two recorders with two histogram
        // 2 - two histogram for storing accumulated values from current phase
        // 1 - temporary histogram used for snapshot extracting
        if (reportUncompletedChunkToSnapshot) {
            return oneHistogramPessimisticFootprint * (chunks.length + 4 + 2 + 1);
        } else {
            return oneHistogramPessimisticFootprint * (chunks.length + 4 + 1);
        }
    }

    private static final class Chunk {

        private final Histogram histogram;
        private volatile long proposedInvalidationTimestamp;

        public Chunk(Histogram histogram, long proposedInvalidationTimestamp) {
            this.histogram = histogram;
            this.proposedInvalidationTimestamp = proposedInvalidationTimestamp;
        }

        @Override
        public String toString() {
            return "Chunk{" +
                    "\n, proposedInvalidationTimestamp=" + proposedInvalidationTimestamp +
                    "\n, histogram=" + Printer.histogramToString(histogram) +
                    "\n}";
        }
    }

    private static final class Phase {

        final Recorder recorder;
        final Histogram totalsHistogram;
        Histogram intervalHistogram;
        volatile long proposedInvalidationTimestamp;

        Phase(Supplier<Recorder> recorderSupplier, long proposedInvalidationTimestamp, boolean reportUncompletedChunkToSnapshot) {
            this.recorder = recorderSupplier.get();
            this.intervalHistogram = recorder.getIntervalHistogram();
            if (reportUncompletedChunkToSnapshot) {
                totalsHistogram = intervalHistogram.copy();
            } else {
                totalsHistogram = null;
            }
            this.proposedInvalidationTimestamp = proposedInvalidationTimestamp;
        }

        @Override
        public String toString() {
            return "Phase{" +
                    "\n, proposedInvalidationTimestamp=" + proposedInvalidationTimestamp +
                    "\n, totalsHistogram=" + (totalsHistogram != null? Printer.histogramToString(totalsHistogram): "null") +
                    "\n, intervalHistogram=" + Printer.histogramToString(intervalHistogram) +
                    "\n}";
        }

    }

    @Override
    public String toString() {
        return "ResetByChunksAccumulator{" +
                "\nintervalBetweenResettingMillis=" + intervalBetweenResettingMillis +
                ",\n creationTimestamp=" + creationTimestamp +
                ",\n reportUncompletedChunkToSnapshot=" + reportUncompletedChunkToSnapshot +
                ",\n chunks=" + Printer.printArray(chunks, "chunk") +
                ",\n clock=" + clock +
                ",\n left=" + left +
                ",\n right=" + right +
                ",\n currentPhase=" + (currentPhaseRef.get() == left? "left": "right") +
                ",\n temporarySnapshotHistogram=" + Printer.histogramToString(temporarySnapshotHistogram)  +
                ",\n activeMutators=" + activeMutators.get() +
                ",\n postponedPhaseRotation=" + postponedPhaseRotation +
                '}';
    }
}
