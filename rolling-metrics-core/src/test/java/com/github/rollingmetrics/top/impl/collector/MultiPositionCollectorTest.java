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

package com.github.rollingmetrics.top.impl.collector;

import com.github.rollingmetrics.top.TestData;
import org.junit.Test;


import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;


public class MultiPositionCollectorTest {

    private PositionCollector collector = new MultiPositionCollector(2);
    private PositionCollector collector2 = new MultiPositionCollector(2);

    @Test
    public void test() {
        PositionCollectorTestUtil.assertEmpty(collector);

        assertTrue(collector.add(TestData.first));
        assertTrue(collector.add(TestData.first));
        PositionCollectorTestUtil.checkOrder(collector, TestData.first);

        assertTrue(collector.add(TestData.second));
        assertFalse(collector.add(TestData.second));
        PositionCollectorTestUtil.checkOrder(collector, TestData.second, TestData.first);

        assertTrue(collector.add(TestData.third));
        assertFalse(collector.add(TestData.third));
        PositionCollectorTestUtil.checkOrder(collector, TestData.third, TestData.second);

        assertFalse(collector.add(TestData.first));
        PositionCollectorTestUtil.checkOrder(collector, TestData.third, TestData.second);
    }

    @Test
    public void testAddInto() {
        collector.addInto(collector2);
        PositionCollectorTestUtil.assertEmpty(collector2);

        collector.add(TestData.first);
        collector.addInto(collector2);
        PositionCollectorTestUtil.checkOrder(collector2, TestData.first);

        collector.add(TestData.second);
        collector.addInto(collector2);
        PositionCollectorTestUtil.checkOrder(collector2, TestData.second, TestData.first);

        collector.add(TestData.third);
        collector.addInto(collector2);
        PositionCollectorTestUtil.checkOrder(collector2, TestData.third, TestData.second);
    }

    @Test
    public void testReset() {
        collector.add(TestData.first);

        collector.reset();
        PositionCollectorTestUtil.assertEmpty(collector);
    }

}