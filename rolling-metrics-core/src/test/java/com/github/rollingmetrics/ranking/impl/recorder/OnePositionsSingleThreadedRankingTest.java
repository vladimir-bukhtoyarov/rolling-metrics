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
import org.junit.Test;


import static com.github.rollingmetrics.ranking.impl.util.RankingTestUtil.update;
import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;


public class OnePositionsSingleThreadedRankingTest {

    private SingleThreadedRanking ranking = new SingleThreadedRanking(1, 0L);
    private SingleThreadedRanking ranking2 = new SingleThreadedRanking(1, 0L);

    @Test
    public void test() {
        PositionCollectorTestUtil.assertEmpty(ranking);

        assertTrue(update(ranking, RankingTestData.first));
        assertFalse(update(ranking, RankingTestData.first));
        PositionCollectorTestUtil.checkOrder(ranking, RankingTestData.first);

        assertTrue(update(ranking, RankingTestData.second));
        assertFalse(update(ranking, RankingTestData.second));
        PositionCollectorTestUtil.checkOrder(ranking, RankingTestData.second);

        assertTrue(update(ranking, RankingTestData.third));
        assertFalse(update(ranking, RankingTestData.third));
        PositionCollectorTestUtil.checkOrder(ranking, RankingTestData.third);

        assertFalse(update(ranking, RankingTestData.first));
        PositionCollectorTestUtil.checkOrder(ranking, RankingTestData.third);
    }

    @Test
    public void testAddInto() {
        ranking.addInto(ranking2);
        PositionCollectorTestUtil.assertEmpty(ranking2);

        update(ranking, RankingTestData.first);
        ranking.addInto(ranking2);
        PositionCollectorTestUtil.checkOrder(ranking2, RankingTestData.first);
    }

    @Test
    public void testReset() {
        update(ranking, RankingTestData.first);

        ranking.reset();
        PositionCollectorTestUtil.assertEmpty(ranking);
    }

}