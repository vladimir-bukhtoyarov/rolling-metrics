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

package com.github.rollingmetrics.top.impl.recorder;

import com.github.rollingmetrics.top.TestData;
import com.github.rollingmetrics.top.impl.collector.PositionCollector;
import com.github.rollingmetrics.top.impl.collector.PositionCollectorTestUtil;
import org.junit.Test;


public class SinglePositionRecorderTest {

    private PositionRecorder recorder = new SinglePositionRecorder(TestData.THRESHOLD_NANOS, 1000);
    private PositionCollector collector = PositionCollector.createCollector(1);

    @Test
    public void test() {
        PositionRecorderTestUtil.assertEmpty(recorder);

        PositionRecorderTestUtil.update(recorder, TestData.first);
        PositionRecorderTestUtil.checkOrder(recorder, TestData.first);

        PositionRecorderTestUtil.update(recorder, TestData.second);
        PositionRecorderTestUtil.checkOrder(recorder, TestData.second);

        PositionRecorderTestUtil.update(recorder, TestData.third);
        PositionRecorderTestUtil.checkOrder(recorder, TestData.third);

        PositionRecorderTestUtil.update(recorder, TestData.first);
        PositionRecorderTestUtil.checkOrder(recorder, TestData.third);
    }

    @Test
    public void testAddInto() {
        recorder.addInto(collector);
        PositionCollectorTestUtil.assertEmpty(collector);

        PositionRecorderTestUtil.update(recorder, TestData.first);
        recorder.addInto(collector);
        PositionCollectorTestUtil.checkOrder(collector, TestData.first);
    }

    @Test
    public void testReset() {
        PositionRecorderTestUtil.update(recorder, TestData.first);

        recorder.reset();
        PositionRecorderTestUtil.assertEmpty(recorder);
    }

}