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

package com.github.metricscore.hdrhistogram;

import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Snapshot;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class OverflowResolverTest {

    @Test
    public void testSkipBigValues() {
        Reservoir reservoir = new HdrBuilder().withHighestTrackableValue(100, OverflowResolver.SKIP).buildReservoir();

        reservoir.update(101);
        Snapshot snapshot = reservoir.getSnapshot();
        assertEquals(0, snapshot.getMax());

        reservoir.update(100);
        snapshot = reservoir.getSnapshot();
        assertEquals(100, snapshot.getMax());

        reservoir.update(99);
        snapshot = reservoir.getSnapshot();
        assertEquals(99, snapshot.getMin());
    }

    @Test
    public void testReduceBigValuesToMax() {
        Reservoir reservoir = new HdrBuilder().withHighestTrackableValue(100, OverflowResolver.REDUCE_TO_HIGHEST_TRACKABLE).buildReservoir();

        reservoir.update(101);
        Snapshot snapshot = reservoir.getSnapshot();
        assertEquals(100, snapshot.getMax());

        reservoir.update(100);
        snapshot = reservoir.getSnapshot();
        assertEquals(100, snapshot.getMax());

        reservoir.update(99);
        snapshot = reservoir.getSnapshot();
        assertEquals(99, snapshot.getMin());
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testPassThruBigValues() {
        Reservoir reservoir = new HdrBuilder().withHighestTrackableValue(100, OverflowResolver.PASS_THRU).buildReservoir();
        reservoir.update(100000);
    }

    @Test
    public void testPassThruBigValues2() {
        Reservoir reservoir = new HdrBuilder()
                .withHighestTrackableValue(100, OverflowResolver.PASS_THRU)
                .buildReservoir();
        reservoir.update(101);
        Snapshot snapshot = reservoir.getSnapshot();
        assertEquals(101, snapshot.getMax());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void sizeMethodShouldBeUndefined() {
        Reservoir reservoir = new HdrBuilder()
                .withHighestTrackableValue(100, OverflowResolver.REDUCE_TO_HIGHEST_TRACKABLE)
                .buildReservoir();
        reservoir.size();
    }

}
