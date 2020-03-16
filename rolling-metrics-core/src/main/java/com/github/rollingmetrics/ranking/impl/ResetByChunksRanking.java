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

package com.github.rollingmetrics.ranking.impl;


import com.github.rollingmetrics.ranking.impl.recorder.ConcurrentRanking;
import com.github.rollingmetrics.ranking.impl.recorder.RankingRecorder;
import com.github.rollingmetrics.ranking.impl.recorder.SingleThreadedRanking;
import com.github.rollingmetrics.util.Printer;
import com.github.rollingmetrics.ranking.Position;
import com.github.rollingmetrics.ranking.Ranking;
import com.github.rollingmetrics.util.Ticker;
import com.github.rollingmetrics.util.ResilientExecutionUtil;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;


public class ResetByChunksRanking implements Ranking {

    private final Executor backgroundExecutor;
    private final long intervalBetweenResettingMillis;
    private final long creationTimestamp;
    private final ArchivedRanking[] archive;
    private final boolean historySupported;
    private final Ticker ticker;
    private final SingleThreadedRanking temporarySnapshotRanking;

    private final Phase left;
    private final Phase right;
    private final Phase[] phases;
    private final AtomicReference<Phase> currentPhaseRef;

    public ResetByChunksRanking(int size, long threshold, long intervalBetweenResettingMillis, int numberHistoryChunks, Ticker ticker, Executor backgroundExecutor) {
        this.intervalBetweenResettingMillis = intervalBetweenResettingMillis;
        this.ticker = ticker;
        this.creationTimestamp = ticker.stableMilliseconds();
        this.backgroundExecutor = backgroundExecutor;

        Supplier<RankingRecorder> recorderSupplier = () -> new RankingRecorder(size, threshold);
        this.left = new Phase(recorderSupplier.get(), creationTimestamp + intervalBetweenResettingMillis);
        this.right = new Phase(recorderSupplier.get(), Long.MAX_VALUE);
        this.phases = new Phase[] {left, right};
        this.currentPhaseRef = new AtomicReference<>(left);

        Supplier<SingleThreadedRanking> collectorSupplier = () -> new SingleThreadedRanking(size, threshold);
        this.historySupported = numberHistoryChunks > 0;
        if (historySupported) {
            this.archive = new ArchivedRanking[numberHistoryChunks];
            for (int i = 0; i < numberHistoryChunks; i++) {
                this.archive[i] = new ArchivedRanking(collectorSupplier.get(), Long.MIN_VALUE);
            }
        } else {
            archive = null;
        }
        this.temporarySnapshotRanking = collectorSupplier.get();
    }

    @Override
    public void update(long weight, Object identity) {
        long currentTimeMillis = ticker.stableMilliseconds();
        Phase currentPhase = currentPhaseRef.get();
        if (currentTimeMillis < currentPhase.proposedInvalidationTimestamp) {
            currentPhase.recorder.update(weight, identity);
            return;
        }

        Phase nextPhase = currentPhase == left ? right : left;
        nextPhase.recorder.update(weight, identity);

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
        temporarySnapshotRanking.reset();
        long currentTimeMillis = ticker.stableMilliseconds();

        if (historySupported) {
            // TODO need to rewrite iteration order from legacy to fresh
            for (ArchivedRanking archivedRanking : archive) {
                if (archivedRanking.proposedInvalidationTimestamp > currentTimeMillis) {
                    archivedRanking.singleThreadedRanking.addInto(temporarySnapshotRanking);
                }
            }
        }

        Phase first = phases[0];
        Phase second = phases[1];
        if (second.proposedInvalidationTimestamp > first.proposedInvalidationTimestamp) {
            first = phases[1];
            second = phases[0];
        }
        for (Phase phase : new Phase[] { first, second }) {
            if (phase.isNeedToBeReportedToSnapshot(currentTimeMillis)) {
                phase.intervalRecorder = phase.recorder.getIntervalRecorder(phase.intervalRecorder);
                phase.intervalRecorder.addIntoUnsafe(phase.totalsCollector);
                phase.totalsCollector.addInto(temporarySnapshotRanking);
            }
        }

        return temporarySnapshotRanking.getPositionsInDescendingOrder();
    }

    @Override
    public int getSize() {
        return left.intervalRecorder.getMaxSize();
    }

    private synchronized void rotate(long currentTimeMillis, Phase currentPhase, Phase nextPhase) {
        try {
            currentPhase.intervalRecorder = currentPhase.recorder.getIntervalRecorder(currentPhase.intervalRecorder);
            currentPhase.intervalRecorder.addIntoUnsafe(currentPhase.totalsCollector);
            if (historySupported) {
                // move values from recorder to correspondent archived collector
                long currentPhaseNumber = (currentPhase.proposedInvalidationTimestamp - creationTimestamp) / intervalBetweenResettingMillis;
                int correspondentArchiveIndex = (int) (currentPhaseNumber - 1) % archive.length;
                ArchivedRanking correspondentArchivedRanking = archive[correspondentArchiveIndex];
                correspondentArchivedRanking.singleThreadedRanking.reset();
                currentPhase.totalsCollector.addInto(correspondentArchivedRanking.singleThreadedRanking);
                correspondentArchivedRanking.proposedInvalidationTimestamp = currentPhase.proposedInvalidationTimestamp + archive.length * intervalBetweenResettingMillis;
            }
            currentPhase.totalsCollector.reset();
        } finally {
            long millisSinceCreation = currentTimeMillis - creationTimestamp;
            long intervalsSinceCreation = millisSinceCreation / intervalBetweenResettingMillis;
            currentPhase.proposedInvalidationTimestamp = Long.MAX_VALUE;
            nextPhase.proposedInvalidationTimestamp = creationTimestamp + (intervalsSinceCreation + 1) * intervalBetweenResettingMillis;
        }
    }

    private final class ArchivedRanking {

        private final SingleThreadedRanking singleThreadedRanking;
        private volatile long proposedInvalidationTimestamp;

        public ArchivedRanking(SingleThreadedRanking singleThreadedRanking, long proposedInvalidationTimestamp) {
            this.singleThreadedRanking = singleThreadedRanking;
            this.proposedInvalidationTimestamp = proposedInvalidationTimestamp;
        }

        @Override
        public String toString() {
            return "ArchivedRanking{" +
                    "\n, proposedInvalidationTimestamp=" + proposedInvalidationTimestamp +
                    "\n, collector=" + singleThreadedRanking +
                    "\n}";
        }
    }

    private final class Phase {

        final RankingRecorder recorder;
        final SingleThreadedRanking totalsCollector;
        ConcurrentRanking intervalRecorder;
        volatile long proposedInvalidationTimestamp;

        Phase(RankingRecorder recorder, long proposedInvalidationTimestamp) {
            this.recorder = recorder;
            this.intervalRecorder = recorder.getIntervalRecorder();
            this.totalsCollector = new SingleThreadedRanking(intervalRecorder.getMaxSize(), intervalRecorder.getThreshold());
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
                ",\n temporarySnapshotCollector=" + temporarySnapshotRanking +
                '}';
    }

}
