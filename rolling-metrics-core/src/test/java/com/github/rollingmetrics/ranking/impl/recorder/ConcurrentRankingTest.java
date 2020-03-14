/*
 *
 *  Copyright 2020 Vladimir Bukhtoyarov
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

package com.github.rollingmetrics.ranking.impl.recorder;

import com.github.rollingmetrics.ranking.impl.util.RankingTestData;
import com.github.rollingmetrics.ranking.impl.util.PositionCollectorTestUtil;
import com.github.rollingmetrics.ranking.impl.util.RankingRecorderTestUtil;
import org.junit.Test;

public class ConcurrentRankingTest {

    private SingleThreadedRanking snaphsot = new SingleThreadedRanking(2, 0L);
    private ConcurrentRanking recorder = new ConcurrentRanking(2, RankingTestData.THRESHOLD_NANOS);

    @Test
    public void test() {
        RankingRecorderTestUtil.assertEmpty(recorder);

        RankingRecorderTestUtil.update(recorder, RankingTestData.first);
        RankingRecorderTestUtil.update(recorder, RankingTestData.first);
        RankingRecorderTestUtil.checkOrder(recorder, RankingTestData.first);

        RankingRecorderTestUtil.update(recorder, RankingTestData.second);
        RankingRecorderTestUtil.update(recorder, RankingTestData.second);
        RankingRecorderTestUtil.checkOrder(recorder, RankingTestData.second, RankingTestData.first);

        RankingRecorderTestUtil.update(recorder, RankingTestData.third);
        RankingRecorderTestUtil.update(recorder, RankingTestData.third);
        RankingRecorderTestUtil.checkOrder(recorder, RankingTestData.third, RankingTestData.second);

        RankingRecorderTestUtil.update(recorder, RankingTestData.first);
        RankingRecorderTestUtil.checkOrder(recorder, RankingTestData.third, RankingTestData.second);
    }

    @Test
    public void testAddInto() {
        recorder.addIntoUnsafe(snaphsot);
        PositionCollectorTestUtil.assertEmpty(snaphsot);

        RankingRecorderTestUtil.update(recorder, RankingTestData.first);
        recorder.addIntoUnsafe(snaphsot);
        PositionCollectorTestUtil.checkOrder(snaphsot, RankingTestData.first);

        RankingRecorderTestUtil.update(recorder, RankingTestData.second);
        recorder.addIntoUnsafe(snaphsot);
        PositionCollectorTestUtil.checkOrder(snaphsot, RankingTestData.second, RankingTestData.first);
    }

    @Test
    public void testReset() {
        RankingRecorderTestUtil.update(recorder, RankingTestData.first);

        recorder.resetUnsafe();
        RankingRecorderTestUtil.assertEmpty(recorder);
    }

}