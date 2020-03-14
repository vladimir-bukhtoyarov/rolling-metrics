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


import com.github.rollingmetrics.ranking.Position;
import com.github.rollingmetrics.ranking.Ranking;
import com.github.rollingmetrics.ranking.impl.recorder.ConcurrentRanking;
import com.github.rollingmetrics.ranking.impl.recorder.RankingRecorder;

import java.util.List;

public class ResetOnSnapshotRanking implements Ranking {

    private final RankingRecorder recorder;
    private ConcurrentRanking intervalRecorder;

    public ResetOnSnapshotRanking(int size, long latencyThresholdNanos) {
        this.recorder = new RankingRecorder(size, latencyThresholdNanos);
        this.intervalRecorder = recorder.getIntervalRecorder();
    }

    @Override
    public void update(long weight, Object identity) {
        recorder.update(weight, identity);
    }

    @Override
    synchronized public List<Position> getPositionsInDescendingOrder() {
        intervalRecorder = recorder.getIntervalRecorder(intervalRecorder);
        return intervalRecorder.getPositionsInDescendingOrderUnsafe();
    }

    @Override
    public int getSize() {
        return intervalRecorder.getMaxSize();
    }

}
