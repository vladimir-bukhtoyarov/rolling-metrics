/*
 *    Copyright 2020 Vladimir Bukhtoyarov
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package com.github.rollingmetrics.top.impl;

import com.github.rollingmetrics.top.TopTestData;
import com.github.rollingmetrics.top.impl.recorder.ConcurrentRanking;
import com.github.rollingmetrics.top.impl.recorder.SingleThreadedRanking;
import org.junit.Test;

public class MultiConcurrentRankingTest {

    private SingleThreadedRanking snaphsot = new SingleThreadedRanking(2);
    private ConcurrentRanking recorder = new ConcurrentRanking(2, TopTestData.THRESHOLD_NANOS);

    @Test
    public void test() {
        PositionRecorderTestUtil.assertEmpty(recorder);

        PositionRecorderTestUtil.update(recorder, TopTestData.first);
        PositionRecorderTestUtil.update(recorder, TopTestData.first);
        PositionRecorderTestUtil.checkOrder(recorder, TopTestData.first);

        PositionRecorderTestUtil.update(recorder, TopTestData.second);
        PositionRecorderTestUtil.update(recorder, TopTestData.second);
        PositionRecorderTestUtil.checkOrder(recorder, TopTestData.second, TopTestData.first);

        PositionRecorderTestUtil.update(recorder, TopTestData.third);
        PositionRecorderTestUtil.update(recorder, TopTestData.third);
        PositionRecorderTestUtil.checkOrder(recorder, TopTestData.third, TopTestData.second);

        PositionRecorderTestUtil.update(recorder, TopTestData.first);
        PositionRecorderTestUtil.checkOrder(recorder, TopTestData.third, TopTestData.second);
    }

    @Test
    public void testAddInto() {
        recorder.addIntoUnsafe(snaphsot);
        PositionCollectorTestUtil.assertEmpty(snaphsot);

        PositionRecorderTestUtil.update(recorder, TopTestData.first);
        recorder.addIntoUnsafe(snaphsot);
        PositionCollectorTestUtil.checkOrder(snaphsot, TopTestData.first);

        PositionRecorderTestUtil.update(recorder, TopTestData.second);
        recorder.addIntoUnsafe(snaphsot);
        PositionCollectorTestUtil.checkOrder(snaphsot, TopTestData.second, TopTestData.first);
    }

    @Test
    public void testReset() {
        PositionRecorderTestUtil.update(recorder, TopTestData.first);

        recorder.resetUnsafe();
        PositionRecorderTestUtil.assertEmpty(recorder);
    }

}