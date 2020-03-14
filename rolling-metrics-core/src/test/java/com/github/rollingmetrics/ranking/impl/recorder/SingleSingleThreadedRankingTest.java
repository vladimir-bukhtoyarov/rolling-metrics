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


public class SingleSingleThreadedRankingTest {

    private SingleThreadedRanking collector = new SingleThreadedRanking(1, 0L);
    private SingleThreadedRanking collector2 = new SingleThreadedRanking(1, 0L);

    @Test
    public void test() {
        PositionCollectorTestUtil.assertEmpty(collector);

        assertTrue(update(collector, RankingTestData.first));
        assertFalse(update(collector, RankingTestData.first));
        PositionCollectorTestUtil.checkOrder(collector, RankingTestData.first);

        assertTrue(update(collector, RankingTestData.second));
        assertFalse(update(collector, RankingTestData.second));
        PositionCollectorTestUtil.checkOrder(collector, RankingTestData.second);

        assertTrue(update(collector, RankingTestData.third));
        assertFalse(update(collector, RankingTestData.third));
        PositionCollectorTestUtil.checkOrder(collector, RankingTestData.third);

        assertFalse(update(collector, RankingTestData.first));
        PositionCollectorTestUtil.checkOrder(collector, RankingTestData.third);
    }

    @Test
    public void testAddInto() {
        collector.addInto(collector2);
        PositionCollectorTestUtil.assertEmpty(collector2);

        update(collector, RankingTestData.first);
        collector.addInto(collector2);
        PositionCollectorTestUtil.checkOrder(collector2, RankingTestData.first);
    }

    @Test
    public void testReset() {
        update(collector, RankingTestData.first);

        collector.reset();
        PositionCollectorTestUtil.assertEmpty(collector);
    }

}