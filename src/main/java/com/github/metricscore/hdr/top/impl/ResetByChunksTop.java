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

package com.github.metricscore.hdr.top.impl;


import com.github.metricscore.hdr.histogram.util.Printer;
import com.github.metricscore.hdr.top.Position;
import com.github.metricscore.hdr.top.Top;
import com.github.metricscore.hdr.top.impl.collector.PositionCollector;
import com.github.metricscore.hdr.top.impl.recorder.PositionRecorder;
import com.github.metricscore.hdr.top.impl.recorder.TwoPhasePositionRecorder;
import com.github.metricscore.hdr.util.Clock;
import com.github.metricscore.hdr.util.ResilientExecutionUtil;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;


public class ResetByChunksTop implements Top {

    private final Executor backgroundExecutor;
    private final long intervalBetweenResettingMillis;
    private final long creationTimestamp;
    private final ArchivedTop[] archive;
    private final boolean historySupported;
    private final Clock clock;
    private final PositionCollector temporarySnapshotCollector;

    private final Phase left;
    private final Phase right;
    private final Phase[] phases;
    private final AtomicReference<Phase> currentPhaseRef;

    public ResetByChunksTop(int size, long slowQueryThresholdNanos, int maxLengthOfQueryDescription, long intervalBetweenResettingMillis, int numberHistoryChunks, Clock clock, Executor backgroundExecutor) {
        this.intervalBetweenResettingMillis = intervalBetweenResettingMillis;
        this.clock = clock;
        this.creationTimestamp = clock.currentTimeMillis();
        this.backgroundExecutor = backgroundExecutor;

        Supplier<TwoPhasePositionRecorder> recorderSupplier = () -> new TwoPhasePositionRecorder(size, slowQueryThresholdNanos, maxLengthOfQueryDescription);
        this.left = new Phase(recorderSupplier.get(), creationTimestamp + intervalBetweenResettingMillis);
        this.right = new Phase(recorderSupplier.get(), Long.MAX_VALUE);
        this.phases = new Phase[] {left, right};
        this.currentPhaseRef = new AtomicReference<>(left);

        Supplier<PositionCollector> collectorSupplier = () -> PositionCollector.createCollector(size);
        this.archive = new ArchivedTop[numberHistoryChunks];
        this.historySupported = numberHistoryChunks > 0;
        if (historySupported) {
            for (int i = 0; i < numberHistoryChunks; i++) {
                this.archive[i] = new ArchivedTop(collectorSupplier.get(), Long.MIN_VALUE);
            }
        }
        this.temporarySnapshotCollector = collectorSupplier.get();
    }

    @Override
    public void update(long timestamp, long latencyTime, TimeUnit latencyUnit, Supplier<String> descriptionSupplier) {
        long currentTimeMillis = clock.currentTimeMillis();
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
        long currentTimeMillis = clock.currentTimeMillis();

        for (Phase phase : phases) {
            if (phase.isNeedToBeReportedToSnapshot(currentTimeMillis)) {
                phase.intervalRecorder = phase.recorder.getIntervalRecorder(phase.intervalRecorder);
                phase.intervalRecorder.addInto(phase.totalsCollector);
                phase.totalsCollector.addInto(temporarySnapshotCollector);
            }
        }
        for (ArchivedTop archivedTop : archive) {
            if (archivedTop.proposedInvalidationTimestamp > currentTimeMillis) {
                archivedTop.collector.addInto(temporarySnapshotCollector);
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
                long currentPhaseNumber = (currentPhase.proposedInvalidationTimestamp - creationTimestamp) / intervalBetweenResettingMillis;
                int correspondentArchiveIndex = (int) (currentPhaseNumber - 1) % archive.length;
                ArchivedTop correspondentArchivedTop = archive[correspondentArchiveIndex];
                correspondentArchivedTop.collector.reset();
                currentPhase.totalsCollector.addInto(correspondentArchivedTop.collector);
                correspondentArchivedTop.proposedInvalidationTimestamp = currentPhase.proposedInvalidationTimestamp + (archive.length - 1) * intervalBetweenResettingMillis;
            }
            currentPhase.totalsCollector.reset();
        } finally {
            long millisSinceCreation = currentTimeMillis - creationTimestamp;
            long intervalsSinceCreation = millisSinceCreation / intervalBetweenResettingMillis;
            currentPhase.proposedInvalidationTimestamp = Long.MAX_VALUE;
            nextPhase.proposedInvalidationTimestamp = creationTimestamp + (intervalsSinceCreation + 1) * intervalBetweenResettingMillis;
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
                ",\n temporarySnapshotCollector=" + temporarySnapshotCollector  +
                '}';
    }

}
