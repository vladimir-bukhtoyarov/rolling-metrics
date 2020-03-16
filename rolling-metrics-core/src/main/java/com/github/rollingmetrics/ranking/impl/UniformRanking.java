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
import com.github.rollingmetrics.ranking.impl.recorder.SingleThreadedRanking;

import java.util.List;

public class UniformRanking<T> implements Ranking<T> {

    private final RankingRecorder phasedRecorder;
    private final SingleThreadedRanking uniformCollector;
    private ConcurrentRanking intervalRecorder;

    public UniformRanking(int size, long threshold) {
        this.phasedRecorder = new RankingRecorder(size, threshold);
        intervalRecorder = phasedRecorder.getIntervalRecorder();
        this.uniformCollector = new SingleThreadedRanking(size, threshold);
    }

    @Override
    public void update(long weight, T identity) {
        phasedRecorder.update(weight, identity);
    }

    @Override
    synchronized public List<Position> getPositionsInDescendingOrder() {
        intervalRecorder = phasedRecorder.getIntervalRecorder(intervalRecorder);
        intervalRecorder.addIntoUnsafe(uniformCollector);
        return uniformCollector.getPositionsInDescendingOrder();
    }

    @Override
    public int getSize() {
        return intervalRecorder.getMaxSize();
    }

}
