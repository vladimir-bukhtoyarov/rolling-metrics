package com.github.metricscore.hdrhistogram;

import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Snapshot;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotSame;

public class ResetOnSnapshotAccumulationStrategyTest {

    @Test
    public void shouldCacheSnapshot() {
        Reservoir reservoir = new HdrBuilder().withAccumulationStrategy(AccumulationStrategy.resetOnSnapshot()).buildReservoir();

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

}