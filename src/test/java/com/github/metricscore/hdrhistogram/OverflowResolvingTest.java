package com.github.metricscore.hdrhistogram;

import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Snapshot;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class OverflowResolvingTest {

    @Test
    public void testSkipBigValues() {
        Reservoir reservoir = new HdrBuilder().withHighestTrackableValue(100, OverflowResolving.SKIP).buildReservoir();

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
        Reservoir reservoir = new HdrBuilder().withHighestTrackableValue(100, OverflowResolving.REDUCE_TO_MAXIMUM).buildReservoir();

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
        Reservoir reservoir = new HdrBuilder().withHighestTrackableValue(100, OverflowResolving.PASS_THRU).buildReservoir();
        reservoir.update(100000);
    }

}
