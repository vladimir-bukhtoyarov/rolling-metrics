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
    private ConcurrentRanking ranking = new ConcurrentRanking(2, 0L);

    @Test
    public void test() {
        RankingRecorderTestUtil.assertEmpty(ranking);

        RankingRecorderTestUtil.update(ranking, RankingTestData.first);
        RankingRecorderTestUtil.checkOrder(ranking, RankingTestData.first);
        RankingRecorderTestUtil.update(ranking, RankingTestData.first);
        RankingRecorderTestUtil.checkOrder(ranking, RankingTestData.first);

        RankingRecorderTestUtil.update(ranking, RankingTestData.second);
        RankingRecorderTestUtil.update(ranking, RankingTestData.second);
        RankingRecorderTestUtil.checkOrder(ranking, RankingTestData.second, RankingTestData.first);

        RankingRecorderTestUtil.update(ranking, RankingTestData.third);
        RankingRecorderTestUtil.update(ranking, RankingTestData.third);
        RankingRecorderTestUtil.checkOrder(ranking, RankingTestData.third, RankingTestData.second);

        RankingRecorderTestUtil.update(ranking, RankingTestData.first);
        RankingRecorderTestUtil.checkOrder(ranking, RankingTestData.third, RankingTestData.second);
    }

    @Test
    public void testAddInto() {
        ranking.addIntoUnsafe(snaphsot);
        PositionCollectorTestUtil.assertEmpty(snaphsot);

        RankingRecorderTestUtil.update(ranking, RankingTestData.first);
        ranking.addIntoUnsafe(snaphsot);
        PositionCollectorTestUtil.checkOrder(snaphsot, RankingTestData.first);

        RankingRecorderTestUtil.update(ranking, RankingTestData.second);
        ranking.addIntoUnsafe(snaphsot);
        PositionCollectorTestUtil.checkOrder(snaphsot, RankingTestData.second, RankingTestData.first);
    }

    @Test
    public void testReset() {
        RankingRecorderTestUtil.update(ranking, RankingTestData.first);

        ranking.resetUnsafe();
        RankingRecorderTestUtil.assertEmpty(ranking);
    }

}