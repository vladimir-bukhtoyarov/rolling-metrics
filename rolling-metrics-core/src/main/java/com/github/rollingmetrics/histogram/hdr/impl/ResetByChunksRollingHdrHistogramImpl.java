/*
 *
 *  Copyright 2017 Vladimir Bukhtoyarov
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

package com.github.rollingmetrics.histogram.hdr.impl;

import com.github.rollingmetrics.histogram.hdr.HdrHistogramUtil;
import com.github.rollingmetrics.histogram.hdr.RecorderSettings;
import com.github.rollingmetrics.histogram.hdr.RollingSnapshot;
import com.github.rollingmetrics.util.ResilientExecutionUtil;
import com.github.rollingmetrics.util.Ticker;
import com.github.rollingmetrics.util.Printer;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class ResetByChunksRollingHdrHistogramImpl extends AbstractRollingHdrHistogram {

    private final Executor backgroundExecutor;
    private final long intervalBetweenResettingMillis;
    private final long creationTimestamp;
    private final ArchivedHistogram[] archive;
    private final boolean historySupported;
    private final Ticker ticker;
    private final Histogram temporarySnapshotHistogram;

    private final Phase left;
    private final Phase right;
    private final Phase[] phases;
    private final AtomicReference<Phase> currentPhaseRef;

    public ResetByChunksRollingHdrHistogramImpl(RecorderSettings recorderSettings, int numberHistoryChunks, long intervalBetweenResettingMillis, Ticker ticker, Executor backgroundExecutor) {
        super(recorderSettings);

        this.intervalBetweenResettingMillis = intervalBetweenResettingMillis;
        this.ticker = ticker;
        this.creationTimestamp = ticker.stableMilliseconds();
        this.backgroundExecutor = backgroundExecutor;

        this.left = new Phase(recorderSettings, creationTimestamp + intervalBetweenResettingMillis);
        this.right = new Phase(recorderSettings, Long.MAX_VALUE);
        this.phases = new Phase[] {left, right};
        this.currentPhaseRef = new AtomicReference<>(left);


        this.historySupported = numberHistoryChunks > 0;
        if (historySupported) {
            this.archive = new ArchivedHistogram[numberHistoryChunks];
            for (int i = 0; i < numberHistoryChunks; i++) {
                Histogram archivedHistogram = HdrHistogramUtil.createNonConcurrentCopy(left.intervalHistogram);
                this.archive[i] = new ArchivedHistogram(archivedHistogram, Long.MIN_VALUE);
            }
        } else {
            this.archive = null;
        }

        this.temporarySnapshotHistogram = HdrHistogramUtil.createNonConcurrentCopy(left.intervalHistogram);
    }

    @Override
    public void recordSingleValueWithExpectedInterval(long value, long expectedIntervalBetweenValueSamples) {
        long currentTimeMillis = ticker.stableMilliseconds();
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
            HdrHistogramUtil.addSecondToFirst(currentPhase.totalsHistogram, currentPhase.intervalHistogram);
            if (historySupported) {
                // move values from recorder to correspondent archived histogram
                long currentPhaseNumber = (currentPhase.proposedInvalidationTimestamp - creationTimestamp) / intervalBetweenResettingMillis;
                int correspondentArchiveIndex = (int) (currentPhaseNumber - 1) % archive.length;
                ArchivedHistogram correspondentArchivedHistogram = archive[correspondentArchiveIndex];
                HdrHistogramUtil.reset(correspondentArchivedHistogram.histogram);
                HdrHistogramUtil.addSecondToFirst(correspondentArchivedHistogram.histogram, currentPhase.totalsHistogram);
                correspondentArchivedHistogram.proposedInvalidationTimestamp = currentPhase.proposedInvalidationTimestamp + archive.length * intervalBetweenResettingMillis;
            }
            HdrHistogramUtil.reset(currentPhase.totalsHistogram);
        } finally {
            long millisSinceCreation = currentTimeMillis - creationTimestamp;
            long intervalsSinceCreation = millisSinceCreation / intervalBetweenResettingMillis;
            currentPhase.proposedInvalidationTimestamp = Long.MAX_VALUE;
            nextPhase.proposedInvalidationTimestamp = creationTimestamp + (intervalsSinceCreation + 1) * intervalBetweenResettingMillis;
        }
    }

    @Override
    public final synchronized RollingSnapshot getSnapshot(Function<Histogram, RollingSnapshot> snapshotTaker) {
        HdrHistogramUtil.reset(temporarySnapshotHistogram);
        long currentTimeMillis = ticker.stableMilliseconds();

        for (Phase phase : phases) {
            if (phase.isNeedToBeReportedToSnapshot(currentTimeMillis)) {
                phase.intervalHistogram = phase.recorder.getIntervalHistogram(phase.intervalHistogram);
                HdrHistogramUtil.addSecondToFirst(phase.totalsHistogram, phase.intervalHistogram);
                HdrHistogramUtil.addSecondToFirst(temporarySnapshotHistogram, phase.totalsHistogram);
            }
        }
        if (historySupported) {
            for (ArchivedHistogram archivedHistogram : archive) {
                if (archivedHistogram.proposedInvalidationTimestamp > currentTimeMillis) {
                    HdrHistogramUtil.addSecondToFirst(temporarySnapshotHistogram, archivedHistogram.histogram);
                }
            }
        }

        return HdrHistogramUtil.getSnapshot(temporarySnapshotHistogram, snapshotTaker);
    }

    @Override
    public int getEstimatedFootprintInBytes() {
        // each histogram has equivalent pessimistic estimation
        int oneHistogramPessimisticFootprint = temporarySnapshotHistogram.getEstimatedFootprintInBytes();

        // 4 - two recorders with two histogram
        // 2 - two histogram for storing accumulated values from current phase
        // 1 - temporary histogram used for snapshot extracting
        return oneHistogramPessimisticFootprint * ((archive != null? archive.length : 0) + 4 + 2 + 1);
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
                    "\n, histogram=" + histogramValuesToString(histogram) +
                    "\n}";
        }
    }

    private final class Phase {

        final Recorder recorder;
        final Histogram totalsHistogram;
        Histogram intervalHistogram;
        volatile long proposedInvalidationTimestamp;

        Phase(RecorderSettings recorderSettings, long proposedInvalidationTimestamp) {
            this.recorder = recorderSettings.buildRecorder();
            this.intervalHistogram = recorder.getIntervalHistogram();
            this.totalsHistogram = intervalHistogram.copy();
            this.proposedInvalidationTimestamp = proposedInvalidationTimestamp;
        }

        @Override
        public String toString() {
            return "Phase{" +
                    "\n, proposedInvalidationTimestamp=" + proposedInvalidationTimestamp +
                    "\n, totalsHistogram=" + (totalsHistogram != null? histogramValuesToString(totalsHistogram): "null") +
                    "\n, intervalHistogram=" + histogramValuesToString(intervalHistogram) +
                    "\n}";
        }

        boolean isNeedToBeReportedToSnapshot(long currentTimeMillis) {
            long proposedInvalidationTimestampLocal = proposedInvalidationTimestamp;
            if (proposedInvalidationTimestampLocal > currentTimeMillis) {
                return true;
            }
            if (!historySupported) {
                return false;
            }
            long correspondentChunkProposedInvalidationTimestamp = proposedInvalidationTimestampLocal + archive.length * intervalBetweenResettingMillis;
            return correspondentChunkProposedInvalidationTimestamp > currentTimeMillis;
        }
    }

    @Override
    public String toString() {
        return "ResetByChunksRollingHdrHistogramImpl{" +
                "\nintervalBetweenResettingMillis=" + intervalBetweenResettingMillis +
                ",\n creationTimestamp=" + creationTimestamp +
                (!historySupported ? "" : ",\n archive=" + Printer.printArray(archive, "chunk")) +
                ",\n ticker=" + ticker +
                ",\n left=" + left +
                ",\n right=" + right +
                ",\n currentPhase=" + (currentPhaseRef.get() == left? "left": "right") +
                ",\n temporarySnapshotHistogram=" + histogramValuesToString(temporarySnapshotHistogram)  +
                '}';
    }

}
