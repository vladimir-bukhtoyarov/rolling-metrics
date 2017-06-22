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

package com.github.rollingmetrics.histogram.accumulator;

import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Snapshot;
import com.github.rollingmetrics.histogram.HdrBuilder;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotSame;

public class ResetOnSnapshotAccumulatorTest {

    @Test
    public void shouldCacheSnapshot() {
        Reservoir reservoir = new HdrBuilder().resetReservoirOnSnapshot().buildReservoir();

        reservoir.update(10);
        reservoir.update(20);
        Snapshot firstSnapshot = reservoir.getSnapshot();

        reservoir.update(30);
        reservoir.update(40);
        Snapshot secondSnapshot = reservoir.getSnapshot();
        assertNotSame(firstSnapshot, secondSnapshot);
        assertEquals(30, secondSnapshot.getMin());
        assertEquals(40, secondSnapshot.getMax());

        reservoir.update(50);
        reservoir.update(60);
        Snapshot thirdSnapshot = reservoir.getSnapshot();
        assertNotSame(secondSnapshot, thirdSnapshot);
        assertEquals(50, thirdSnapshot.getMin());
        assertEquals(60, thirdSnapshot.getMax());
    }

    @Test
    public void testToString() {
        new HdrBuilder().resetReservoirOnSnapshot().buildReservoir().toString();
    }

}