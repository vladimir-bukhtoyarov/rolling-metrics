/*
 *
 *  Copyright 2016 Vladimir Bukhtoyarov
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

package com.github.metricscore.hdr.top.impl.collector;

import org.junit.Test;


import static com.github.metricscore.hdr.top.TestData.*;
import static com.github.metricscore.hdr.top.impl.collector.PositionCollectorTestUtil.assertEmpty;
import static com.github.metricscore.hdr.top.impl.collector.PositionCollectorTestUtil.checkOrder;
import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;


public class MultiPositionCollectorTest {

    private PositionCollector collector = new MultiPositionCollector(2);
    private PositionCollector collector2 = new MultiPositionCollector(2);

    @Test
    public void test() {
        assertEmpty(collector);

        assertTrue(collector.add(first));
        assertTrue(collector.add(first));
        checkOrder(collector, first);

        assertTrue(collector.add(second));
        assertFalse(collector.add(second));
        checkOrder(collector, second, first);

        assertTrue(collector.add(third));
        assertFalse(collector.add(third));
        checkOrder(collector, third, second);

        assertFalse(collector.add(first));
        checkOrder(collector, third, second);
    }

    @Test
    public void testAddInto() {
        collector.addInto(collector2);
        assertEmpty(collector2);

        collector.add(first);
        collector.addInto(collector2);
        checkOrder(collector2, first);

        collector.add(second);
        collector.addInto(collector2);
        checkOrder(collector2, second, first);

        collector.add(third);
        collector.addInto(collector2);
        checkOrder(collector2, third, second);
    }

    @Test
    public void testReset() {
        collector.add(first);

        collector.reset();
        assertEmpty(collector);
    }

}