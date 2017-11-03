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

package com.github.rollingmetrics.top.impl;


import com.github.rollingmetrics.retention.ResetPeriodicallyByChunksRetentionPolicy;
import com.github.rollingmetrics.retention.ResetPeriodicallyRetentionPolicy;
import com.github.rollingmetrics.top.TopRecorderSettings;
import com.github.rollingmetrics.util.Printer;
import com.github.rollingmetrics.top.Position;
import com.github.rollingmetrics.top.Top;
import com.github.rollingmetrics.top.impl.collector.PositionCollector;
import com.github.rollingmetrics.top.impl.recorder.PositionRecorder;
import com.github.rollingmetrics.top.impl.recorder.TwoPhasePositionRecorder;
import com.github.rollingmetrics.util.Ticker;
import com.github.rollingmetrics.util.ResilientExecutionUtil;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;


public class ResetByChunksTop implements Top {

    public static final long MIN_CHUNK_RESETTING_INTERVAL_MILLIS = 1000;
    public static final int MAX_CHUNKS = 25;

    private final Executor backgroundExecutor;
    private final long intervalBetweenResettingOneChunkMillis;
    private final long creationTimestamp;
    private final ArchivedTop[] archive;
    private final boolean historySupported;
    private final Ticker ticker;
    private final PositionCollector temporarySnapshotCollector;

    private final Phase left;
    private final Phase right;
    private final Phase[] phases;
    private final AtomicReference<Phase> currentPhaseRef;

    public ResetByChunksTop(TopRecorderSettings settings, ResetPeriodicallyRetentionPolicy retentionPolicy, Ticker ticker) {
        this(settings, retentionPolicy.getResettingPeriodMillis(), 0, ticker);
    }

    public ResetByChunksTop(TopRecorderSettings settings, ResetPeriodicallyByChunksRetentionPolicy retentionPolicy, Ticker ticker) {
        this(settings, retentionPolicy.getIntervalBetweenResettingOneChunkMillis(), retentionPolicy.getNumberChunks(), ticker);
    }

    private ResetByChunksTop(TopRecorderSettings settings, long intervalBetweenResettingOneChunkMillis, int numberHistoryChunks, Ticker ticker) {
        if (intervalBetweenResettingOneChunkMillis < ResetByChunksTop.MIN_CHUNK_RESETTING_INTERVAL_MILLIS) {
            String msg = "interval between resetting one chunk should be >= " + ResetByChunksTop.MIN_CHUNK_RESETTING_INTERVAL_MILLIS + " millis";
            throw new IllegalArgumentException(msg);
        }
        if (numberHistoryChunks > ResetByChunksTop.MAX_CHUNKS) {
            throw new IllegalArgumentException("numberHistoryChunks should be <= " + ResetByChunksTop.MAX_CHUNKS);
        }

        this.intervalBetweenResettingOneChunkMillis = intervalBetweenResettingOneChunkMillis;
        this.ticker = ticker;
        this.creationTimestamp = ticker.stableMilliseconds();
        this.backgroundExecutor = settings.getBackgroundExecutor();

        Supplier<TwoPhasePositionRecorder> recorderSupplier = () -> new TwoPhasePositionRecorder(settings.getSize(), settings.getLatencyThreshold().toNanos(), settings.getMaxDescriptionLength());
        this.left = new Phase(recorderSupplier.get(), creationTimestamp + this.intervalBetweenResettingOneChunkMillis);
        this.right = new Phase(recorderSupplier.get(), Long.MAX_VALUE);
        this.phases = new Phase[] {left, right};
        this.currentPhaseRef = new AtomicReference<>(left);

        Supplier<PositionCollector> collectorSupplier = () -> PositionCollector.createCollector(settings.getSize());
        this.historySupported = numberHistoryChunks > 0;
        if (historySupported) {
            this.archive = new ArchivedTop[numberHistoryChunks];
            for (int i = 0; i < numberHistoryChunks; i++) {
                this.archive[i] = new ArchivedTop(collectorSupplier.get(), Long.MIN_VALUE);
            }
        } else {
            archive = null;
        }
        this.temporarySnapshotCollector = collectorSupplier.get();
    }

    @Override
    public void update(long timestamp, long latencyTime, TimeUnit latencyUnit, Supplier<String> descriptionSupplier) {
        long currentTimeMillis = ticker.stableMilliseconds();
        Phase currentPhase = currentPhaseRef.get();
        if (currentTimeMillis < currentPhase.proposedInvalidationTimestamp) {
            currentPhase.recorder.update(timestamp, latencyTime, latencyUnit, descriptionSupplier);
            return;
        }

        Phase nextPhase = currentPhase == left ? right : left;
        nextPhase.recorder.update(timestamp, latencyTime, latencyUnit, descriptionSupplier);

        if (!currentPhaseRef.compareAndSet(currentPhase, nextPhase)) {
            // another writer achieved progress and must submit rotation task to backgroundExecutor
            return;
        }

        // Current thread is responsible to rotate phases.
        Runnable phaseRotation = () -> rotate(currentTimeMillis, currentPhase, nextPhase);
        ResilientExecutionUtil.getInstance().execute(backgroundExecutor, phaseRotation);
    }

    @Override
    synchronized public List<Position> getPositionsInDescendingOrder() {
        temporarySnapshotCollector.reset();
        long currentTimeMillis = ticker.stableMilliseconds();

        for (Phase phase : phases) {
            if (phase.isNeedToBeReportedToSnapshot(currentTimeMillis)) {
                phase.intervalRecorder = phase.recorder.getIntervalRecorder(phase.intervalRecorder);
                phase.intervalRecorder.addInto(phase.totalsCollector);
                phase.totalsCollector.addInto(temporarySnapshotCollector);
            }
        }

        if (historySupported) {
            for (ArchivedTop archivedTop : archive) {
                if (archivedTop.proposedInvalidationTimestamp > currentTimeMillis) {
                    archivedTop.collector.addInto(temporarySnapshotCollector);
                }
            }
        }

        return temporarySnapshotCollector.getPositionsInDescendingOrder();
    }

    @Override
    public int getSize() {
        return left.intervalRecorder.getSize();
    }

    private synchronized void rotate(long currentTimeMillis, Phase currentPhase, Phase nextPhase) {
        try {
            currentPhase.intervalRecorder = currentPhase.recorder.getIntervalRecorder(currentPhase.intervalRecorder);
            currentPhase.intervalRecorder.addInto(currentPhase.totalsCollector);
            if (historySupported) {
                // move values from recorder to correspondent archived collector
                long currentPhaseNumber = (currentPhase.proposedInvalidationTimestamp - creationTimestamp) / intervalBetweenResettingOneChunkMillis;
                int correspondentArchiveIndex = (int) (currentPhaseNumber - 1) % archive.length;
                ArchivedTop correspondentArchivedTop = archive[correspondentArchiveIndex];
                correspondentArchivedTop.collector.reset();
                currentPhase.totalsCollector.addInto(correspondentArchivedTop.collector);
                correspondentArchivedTop.proposedInvalidationTimestamp = currentPhase.proposedInvalidationTimestamp + archive.length * intervalBetweenResettingOneChunkMillis;
            }
            currentPhase.totalsCollector.reset();
        } finally {
            long millisSinceCreation = currentTimeMillis - creationTimestamp;
            long intervalsSinceCreation = millisSinceCreation / intervalBetweenResettingOneChunkMillis;
            currentPhase.proposedInvalidationTimestamp = Long.MAX_VALUE;
            nextPhase.proposedInvalidationTimestamp = creationTimestamp + (intervalsSinceCreation + 1) * intervalBetweenResettingOneChunkMillis;
        }
    }

    private final class ArchivedTop {

        private final PositionCollector collector;
        private volatile long proposedInvalidationTimestamp;

        public ArchivedTop(PositionCollector collector, long proposedInvalidationTimestamp) {
            this.collector = collector;
            this.proposedInvalidationTimestamp = proposedInvalidationTimestamp;
        }

        @Override
        public String toString() {
            return "ArchivedTop{" +
                    "\n, proposedInvalidationTimestamp=" + proposedInvalidationTimestamp +
                    "\n, collector=" + collector +
                    "\n}";
        }
    }

    private final class Phase {

        final TwoPhasePositionRecorder recorder;
        final PositionCollector totalsCollector;
        PositionRecorder intervalRecorder;
        volatile long proposedInvalidationTimestamp;

        Phase(TwoPhasePositionRecorder recorder, long proposedInvalidationTimestamp) {
            this.recorder = recorder;
            this.intervalRecorder = recorder.getIntervalRecorder();
            this.totalsCollector = PositionCollector.createCollector(intervalRecorder.getSize());
            this.proposedInvalidationTimestamp = proposedInvalidationTimestamp;
        }

        @Override
        public String toString() {
            return "Phase{" +
                    "\n, proposedInvalidationTimestamp=" + proposedInvalidationTimestamp +
                    "\n, totalsCollector=" + totalsCollector +
                    "\n, intervalRecorder=" + intervalRecorder +
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
            long correspondentChunkProposedInvalidationTimestamp = proposedInvalidationTimestampLocal + archive.length * intervalBetweenResettingOneChunkMillis;
            return correspondentChunkProposedInvalidationTimestamp > currentTimeMillis;
        }
    }

    @Override
    public String toString() {
        return "ResetByChunksRollingHdrHistogramImpl{" +
                "\nintervalBetweenResettingMillis=" + intervalBetweenResettingOneChunkMillis +
                ",\n creationTimestamp=" + creationTimestamp +
                (!historySupported ? "" : ",\n archive=" + Printer.printArray(archive, "chunk")) +
                ",\n ticker=" + ticker +
                ",\n left=" + left +
                ",\n right=" + right +
                ",\n currentPhase=" + (currentPhaseRef.get() == left? "left": "right") +
                ",\n temporarySnapshotCollector=" + temporarySnapshotCollector  +
                '}';
    }

}
