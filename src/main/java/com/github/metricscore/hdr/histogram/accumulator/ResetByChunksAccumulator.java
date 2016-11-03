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

import com.github.metricscore.hdr.histogram.util.HistogramUtil;
import com.github.metricscore.hdr.util.ResilientExecutionUtil;
import com.github.metricscore.hdr.util.Clock;
import com.codahale.metrics.Snapshot;
import com.github.metricscore.hdr.histogram.util.Printer;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

public class ResetByChunksAccumulator implements Accumulator {

    private final Executor backgroundExecutor;
    private final long intervalBetweenResettingMillis;
    private final long creationTimestamp;
    private final ArchivedHistogram[] archive;
    private final boolean historySupported;
    private final Clock clock;
    private final Histogram temporarySnapshotHistogram;

    private final Phase left;
    private final Phase right;
    private final Phase[] phases;
    private final AtomicReference<Phase> currentPhaseRef;

    public ResetByChunksAccumulator(Supplier<Recorder> recorderSupplier, int numberHistoryChunks, long intervalBetweenResettingMillis, Clock clock, Executor backgroundExecutor) {
        this.intervalBetweenResettingMillis = intervalBetweenResettingMillis;
        this.clock = clock;
        this.creationTimestamp = clock.currentTimeMillis();
        this.backgroundExecutor = backgroundExecutor;

        this.left = new Phase(recorderSupplier, creationTimestamp + intervalBetweenResettingMillis);
        this.right = new Phase(recorderSupplier, Long.MAX_VALUE);
        this.phases = new Phase[] {left, right};
        this.currentPhaseRef = new AtomicReference<>(left);

        this.archive = new ArchivedHistogram[numberHistoryChunks];
        this.historySupported = numberHistoryChunks > 0;
        if (historySupported) {
            for (int i = 0; i < numberHistoryChunks; i++) {
                Histogram archivedHistogram = HistogramUtil.createNonConcurrentCopy(left.intervalHistogram);
                this.archive[i] = new ArchivedHistogram(archivedHistogram, Long.MIN_VALUE);
            }
        }

        this.temporarySnapshotHistogram = HistogramUtil.createNonConcurrentCopy(left.intervalHistogram);
    }

    @Override
    public void recordSingleValueWithExpectedInterval(long value, long expectedIntervalBetweenValueSamples) {
        long currentTimeMillis = clock.currentTimeMillis();
        Phase currentPhase = currentPhaseRef.get();
        if (currentTimeMillis < currentPhase.proposedInvalidationTimestamp) {
            currentPhase.recorder.recordValueWithExpectedInterval(value, expectedIntervalBetweenValueSamples);
            return;
        }

        Phase nextPhase = currentPhase == left ? right : left;
        nextPhase.recorder.recordValueWithExpectedInterval(value, expectedIntervalBetweenValueSamples);

        if (!currentPhaseRef.compareAndSet(currentPhase, nextPhase)) {
            // another writer achieved progress and must submit rotation task to backgroundExecutor
            return;
        }

        // Current thread is responsible to rotate phases.
        Runnable phaseRotation = () -> rotate(currentTimeMillis, currentPhase, nextPhase);
        ResilientExecutionUtil.getInstance().execute(backgroundExecutor, phaseRotation);
    }

    private synchronized void rotate(long currentTimeMillis, Phase currentPhase, Phase nextPhase) {
        try {
            currentPhase.intervalHistogram = currentPhase.recorder.getIntervalHistogram(currentPhase.intervalHistogram);
            HistogramUtil.addSecondToFirst(currentPhase.totalsHistogram, currentPhase.intervalHistogram);
            if (historySupported) {
                // move values from recorder to correspondent archived histogram
                long currentPhaseNumber = (currentPhase.proposedInvalidationTimestamp - creationTimestamp) / intervalBetweenResettingMillis;
                int correspondentArchiveIndex = (int) (currentPhaseNumber - 1) % archive.length;
                ArchivedHistogram correspondentArchivedHistogram = archive[correspondentArchiveIndex];
                HistogramUtil.reset(correspondentArchivedHistogram.histogram);
                HistogramUtil.addSecondToFirst(correspondentArchivedHistogram.histogram, currentPhase.totalsHistogram);
                correspondentArchivedHistogram.proposedInvalidationTimestamp = currentPhase.proposedInvalidationTimestamp + (archive.length - 1) * intervalBetweenResettingMillis;
            }
            HistogramUtil.reset(currentPhase.totalsHistogram);
        } finally {
            long millisSinceCreation = currentTimeMillis - creationTimestamp;
            long intervalsSinceCreation = millisSinceCreation / intervalBetweenResettingMillis;
            currentPhase.proposedInvalidationTimestamp = Long.MAX_VALUE;
            nextPhase.proposedInvalidationTimestamp = creationTimestamp + (intervalsSinceCreation + 1) * intervalBetweenResettingMillis;
        }
    }

    @Override
    public final synchronized Snapshot getSnapshot(Function<Histogram, Snapshot> snapshotTaker) {
        HistogramUtil.reset(temporarySnapshotHistogram);
        long currentTimeMillis = clock.currentTimeMillis();

        for (Phase phase : phases) {
            if (phase.isNeedToBeReportedToSnapshot(currentTimeMillis)) {
                phase.intervalHistogram = phase.recorder.getIntervalHistogram(phase.intervalHistogram);
                HistogramUtil.addSecondToFirst(phase.totalsHistogram, phase.intervalHistogram);
                HistogramUtil.addSecondToFirst(temporarySnapshotHistogram, phase.totalsHistogram);
            }
        }
        for (ArchivedHistogram archivedHistogram : archive) {
            if (archivedHistogram.proposedInvalidationTimestamp > currentTimeMillis) {
                HistogramUtil.addSecondToFirst(temporarySnapshotHistogram, archivedHistogram.histogram);
            }
        }

        return HistogramUtil.getSnapshot(temporarySnapshotHistogram, snapshotTaker);
    }

    @Override
    public int getEstimatedFootprintInBytes() {
        // each histogram has equivalent pessimistic estimation
        int oneHistogramPessimisticFootprint = temporarySnapshotHistogram.getEstimatedFootprintInBytes();

        // 4 - two recorders with two histogram
        // 2 - two histogram for storing accumulated values from current phase
        // 1 - temporary histogram used for snapshot extracting
        return oneHistogramPessimisticFootprint * (archive.length + 4 + 2 + 1);
    }

    private final class ArchivedHistogram {

        private final Histogram histogram;
        private volatile long proposedInvalidationTimestamp;

        public ArchivedHistogram(Histogram histogram, long proposedInvalidationTimestamp) {
            this.histogram = histogram;
            this.proposedInvalidationTimestamp = proposedInvalidationTimestamp;
        }

        @Override
        public String toString() {
            return "ArchivedHistogram{" +
                    "\n, proposedInvalidationTimestamp=" + proposedInvalidationTimestamp +
                    "\n, histogram=" + Printer.histogramToString(histogram) +
                    "\n}";
        }
    }

    private final class Phase {

        final Recorder recorder;
        final Histogram totalsHistogram;
        Histogram intervalHistogram;
        volatile long proposedInvalidationTimestamp;

        Phase(Supplier<Recorder> recorderSupplier, long proposedInvalidationTimestamp) {
            this.recorder = recorderSupplier.get();
            this.intervalHistogram = recorder.getIntervalHistogram();
            this.totalsHistogram = intervalHistogram.copy();
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

        boolean isNeedToBeReportedToSnapshot(long currentTimeMillis) {
            long proposedInvalidationTimestampLocal = proposedInvalidationTimestamp;
            if (proposedInvalidationTimestampLocal > currentTimeMillis) {
                return true;
            }
            long correspondentChunkProposedInvalidationTimestamp = proposedInvalidationTimestampLocal + (archive.length - 1) * intervalBetweenResettingMillis;
            return correspondentChunkProposedInvalidationTimestamp > currentTimeMillis;
        }
    }

    @Override
    public String toString() {
        return "ResetByChunksAccumulator{" +
                "\nintervalBetweenResettingMillis=" + intervalBetweenResettingMillis +
                ",\n creationTimestamp=" + creationTimestamp +
                (!historySupported ? "" : ",\n archive=" + Printer.printArray(archive, "chunk")) +
                ",\n clock=" + clock +
                ",\n left=" + left +
                ",\n right=" + right +
                ",\n currentPhase=" + (currentPhaseRef.get() == left? "left": "right") +
                ",\n temporarySnapshotHistogram=" + Printer.histogramToString(temporarySnapshotHistogram)  +
                '}';
    }

}
