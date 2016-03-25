package com.github.metricscore.hdrhistogram;

import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Snapshot;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotSame;

public class UniformSnapshotAccumulationStrategyTest {

    @Test
    public void shouldCacheSnapshot() {
        Reservoir reservoir = new HdrBuilder().withAccumulationStrategy(AccumulationStrategy.uniform()).buildReservoir();

        reservoir.update(10);
        reservoir.update(20);
        Snapshot firstSnapshot = reservoir.getSnapshot();

        reservoir.update(30);
        reservoir.update(40);
        Snapshot secondSnapshot = reservoir.getSnapshot();
        assertNotSame(firstSnapshot, secondSnapshot);
        assertEquals(10, secondSnapshot.getMin());
        assertEquals(40, secondSnapshot.getMax());

        reservoir.update(9);
        reservoir.update(60);
        Snapshot thirdSnapshot = reservoir.getSnapshot();
        assertNotSame(secondSnapshot, thirdSnapshot);
        assertEquals(9, thirdSnapshot.getMin());
        assertEquals(60, thirdSnapshot.getMax());
    }

}