package com.github.metricscore.hdrhistogram;

import com.codahale.metrics.Reservoir;
import org.junit.Test;

import java.time.Duration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HdrBuilderCacheDecoratorConstructionTest {

    @Test
    public void whenCachingDurationSpecifiedThenReservoirShouldBeDecoratedByProxy() {
        Reservoir reservoir = new HdrBuilder().withSnapshotCachingDuration(Duration.ofSeconds(5)).buildReservoir();
        assertTrue(reservoir instanceof SnapshotCachingReservoir);
    }

    @Test
    public void zeroDurationShouldNotLeadToCreateDecorator() {
        Reservoir reservoir = new HdrBuilder().withSnapshotCachingDuration(Duration.ZERO).buildReservoir();
        assertFalse(reservoir instanceof SnapshotCachingReservoir);
    }

    @Test
    public void byDefaultCachingShouldBeTurnedOf() {
        Reservoir reservoir = new HdrBuilder().buildReservoir();
        assertFalse(reservoir instanceof SnapshotCachingReservoir);
    }

}
