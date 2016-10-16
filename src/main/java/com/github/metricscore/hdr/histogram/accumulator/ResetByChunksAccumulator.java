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

import com.github.metricscore.hdr.util.Clock;
import com.codahale.metrics.Snapshot;
import com.github.metricscore.hdr.histogram.util.Printer;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ResetByChunksAccumulator implements Accumulator {

    private final long intervalBetweenResettingMillis;
    private final boolean reportUncompletedChunkToSnapshot;
    private final Clock clock;
    private final long creationTimestamp;
    private final Histogram temporarySnapshotHistogram;
    private final LeftRightChunk[] chunks;

    public ResetByChunksAccumulator(Supplier<Recorder> recorderSupplier, int numberChunks, long intervalBetweenResettingMillis, boolean reportUncompletedChunkToSnapshot, Clock clock) {
        this.intervalBetweenResettingMillis = intervalBetweenResettingMillis;
        this.clock = clock;
        this.creationTimestamp = clock.currentTimeMillis();
        this.reportUncompletedChunkToSnapshot = reportUncompletedChunkToSnapshot;

        this.chunks = new LeftRightChunk[numberChunks];
        for (int i = 0; i < chunks.length; i++) {
            this.chunks[i] = new LeftRightChunk(recorderSupplier, i);
        }
        this.temporarySnapshotHistogram = chunks[0].left.runningTotals.copy();
    }

    @Override
    public void recordSingleValueWithExpectedInterval(long value, long expectedIntervalBetweenValueSamples) {
        long nowMillis = clock.currentTimeMillis();
        long millisSinceCreation = nowMillis - creationTimestamp;
        long intervalsSinceCreation = millisSinceCreation / intervalBetweenResettingMillis;
        int chunkIndex = (int) intervalsSinceCreation % chunks.length;
        chunks[chunkIndex].recordValue(value, expectedIntervalBetweenValueSamples, nowMillis);
    }

    @Override
    public synchronized Snapshot getSnapshot(Function<Histogram, Snapshot> snapshotTaker) {
        long currentTimeMillis = clock.currentTimeMillis();
        temporarySnapshotHistogram.reset();
        boolean wasInterrupted = false;
        for (LeftRightChunk chunk : chunks) {
            wasInterrupted = wasInterrupted || chunk.addActivePhaseToSnapshot(temporarySnapshotHistogram, currentTimeMillis);
        }
        Snapshot snapshot = snapshotTaker.apply(temporarySnapshotHistogram);
        if (wasInterrupted) {
            Thread.currentThread().interrupt();
        }
        return snapshot;
    }

    @Override
    public int getEstimatedFootprintInBytes() {
        // each histogram has equivalent pessimistic estimation
        int oneHistogramPessimisticFootprint = temporarySnapshotHistogram.getEstimatedFootprintInBytes();

        // 2 phases per each chunk
        // 3 histograms per each chunk
        // 1 - temporary histogram used for snapshot extracting
        return oneHistogramPessimisticFootprint * (chunks.length * 2 * 3 + 1);
    }

    private final class LeftRightChunk {

        final Phase left;
        final Phase right;
        final AtomicReference<Phase> currentPhaseRef;
        final AtomicInteger phaseMutators = new AtomicInteger(0);
        volatile Runnable postponedPhaseRotation = null;
        volatile Thread snapshotTakerThread = null;

        LeftRightChunk(Supplier<Recorder> recorderSupplier, int chunkIndex) {
            left = new Phase(recorderSupplier, creationTimestamp + (chunks.length + chunkIndex) * intervalBetweenResettingMillis);
            right = new Phase(recorderSupplier, Long.MAX_VALUE);
            this.currentPhaseRef = new AtomicReference<>(left);
        }

        boolean addActivePhaseToSnapshot(Histogram snapshotHistogram, long currentTimeMillis) {
            Thread currentThread = Thread.currentThread();
            boolean wasInterrupted = false;

            // Save reference to current currentThread before increment of atomic,
            // it will provide guarantee that snapshot taker will be visible by writers
            this.snapshotTakerThread = currentThread;

            if (phaseMutators.incrementAndGet() > 1) {
                // phase rotation process is in progress by writer thread, it is need to park and wait permit from writer
                do {
                    LockSupport.park(this);
                    wasInterrupted = wasInterrupted || Thread.interrupted();
                    // Due to possibility of spurious wake up we need to wait in loop
                } while (phaseMutators.get() > 1);
            }
            try {
                Phase currentPhase = currentPhaseRef.get();
                currentPhase.addItselfToSnapshot(snapshotHistogram, currentTimeMillis);
            } finally {
                if (phaseMutators.decrementAndGet() > 0) {
                    // the writer thread postponed rotation in order to provide for current thread ability to complete snapshot,
                    // so current thread need to complete rotation by itself
                    Runnable postponedPhaseRotation;
                    do {
                        LockSupport.park(this);
                        wasInterrupted = wasInterrupted || Thread.interrupted();
                    } while ((postponedPhaseRotation = this.postponedPhaseRotation) == null);
                    postponedPhaseRotation.run();
                }
            }
            this.snapshotTakerThread = null;
            return wasInterrupted;
        }

        void recordValue(long value, long expectedIntervalBetweenValueSamples, long currentTimeMillis) {
            Phase currentPhase = currentPhaseRef.get();
            if (currentTimeMillis < currentPhase.proposedInvalidationTimestamp) {
                currentPhase.recorder.recordValueWithExpectedInterval(value, expectedIntervalBetweenValueSamples);
                return;
            }

            Phase nextPhase = currentPhase == left ? right : left;
            if (!currentPhaseRef.compareAndSet(currentPhase, nextPhase)) {
                // another writer achieved progress and must clear current phase data, current writer tread just can write value to next phase and return
                nextPhase.recorder.recordValueWithExpectedInterval(value, expectedIntervalBetweenValueSamples);
                return;
            }

            // Current thread is responsible to rotate phases.
            Runnable phaseRotation = () -> {
                try {
                    postponedPhaseRotation = null;
                    currentPhase.recorder.reset();
                    currentPhase.runningTotals.reset();
                    currentPhase.proposedInvalidationTimestamp = Long.MAX_VALUE;
                    nextPhase.recorder.recordValueWithExpectedInterval(value, expectedIntervalBetweenValueSamples);
                } finally {
                    if (phaseMutators.decrementAndGet() > 0) {
                        // snapshot taker wait permit from current thread
                        LockSupport.unpark(this.snapshotTakerThread);
                    }
                    long millisSinceCreation = currentTimeMillis - creationTimestamp;
                    long intervalsSinceCreation = millisSinceCreation / intervalBetweenResettingMillis;
                    nextPhase.proposedInvalidationTimestamp = creationTimestamp + (intervalsSinceCreation + chunks.length) * intervalBetweenResettingMillis;
                }
            };

            // Need to be aware about snapshot takers in the middle of progress state
            if (phaseMutators.incrementAndGet() > 1) {
                // give chance to snapshot taker to finalize snapshot extraction, rotation will be complete by snapshot taker thread
                postponedPhaseRotation = phaseRotation;
                LockSupport.unpark(this.snapshotTakerThread);
            } else {
                // There are no active snapshot takers in the progress state, lets exchange phases in this writer thread
                phaseRotation.run();
            }
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("LeftRightChunk{");
            sb.append("currentPhase is ").append(currentPhaseRef.get() == left? "left" : "right");
            sb.append(", left=").append(left);
            sb.append(", right=").append(right);
            sb.append(", phaseMutators=").append(phaseMutators);
            sb.append(", postponedPhaseRotation=").append(postponedPhaseRotation);
            sb.append(", snapshotTakerThread=").append(snapshotTakerThread);
            sb.append('}');
            return sb.toString();
        }
    }

    private final class Phase {

        final Histogram runningTotals;
        final Recorder recorder;

        Histogram intervalHistogram;
        volatile long proposedInvalidationTimestamp;

        Phase(Supplier<Recorder> recorderSupplier, long proposedInvalidationTimestamp) {
            this.recorder = recorderSupplier.get();
            this.intervalHistogram = recorder.getIntervalHistogram();
            this.runningTotals = intervalHistogram.copy();
            this.proposedInvalidationTimestamp = proposedInvalidationTimestamp;
        }

        void addItselfToSnapshot(Histogram snapshotHistogram, long currentTimeMillis) {
            long proposedInvalidationTimestamp = this.proposedInvalidationTimestamp;
            if (currentTimeMillis >= proposedInvalidationTimestamp) {
                // The chunk was unused by writers for a long time
                return;
            }

            if (!reportUncompletedChunkToSnapshot) {
                if (currentTimeMillis < proposedInvalidationTimestamp - (chunks.length - 1) * intervalBetweenResettingMillis) {
                    // By configuration we should not add phase to snapshot until it fully completed
                    return;
                }
            }

            intervalHistogram = recorder.getIntervalHistogram(intervalHistogram);
            runningTotals.add(intervalHistogram);
            snapshotHistogram.add(runningTotals);
        }

        @Override
        public String toString() {
            return "Phase{" +
                    "\n, proposedInvalidationTimestamp=" + proposedInvalidationTimestamp +
                    "\n, runningTotals=" + (runningTotals != null? Printer.histogramToString(runningTotals): "null") +
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
                ",\n temporarySnapshotHistogram=" + Printer.histogramToString(temporarySnapshotHistogram) +
                '}';
    }

}
