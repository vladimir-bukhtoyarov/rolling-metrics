package com.github.metricscore.hdrhistogram;

import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Snapshot;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.*;

public class SnapshotCachingTest {

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

    @Test(expected = UnsupportedOperationException.class)
    public void sizeMethodShouldBeUndefined() {
        Reservoir reservoir = new HdrBuilder().withSnapshotCachingDuration(Duration.ofSeconds(10)).buildReservoir();
        reservoir.size();
    }

    @Test
    public void shouldCacheSnapshot() {
        AtomicLong time = new AtomicLong(System.currentTimeMillis());
        WallClock wallClock = WallClock.mock(time);
        Reservoir reservoir = new HdrBuilder(wallClock).withSnapshotCachingDuration(Duration.ofMillis(1000)).buildReservoir();

        reservoir.update(10);
        reservoir.update(20);
        Snapshot firstSnapshot = reservoir.getSnapshot();

        time.getAndAdd(900);
        reservoir.update(30);
        reservoir.update(40);
        Snapshot firstCachedSnapshot = reservoir.getSnapshot();
        assertSame(firstSnapshot, firstCachedSnapshot);
        assertEquals(10, firstCachedSnapshot.getMin());
        assertEquals(20, firstCachedSnapshot.getMax());

        time.getAndAdd(99);
        reservoir.update(50);
        reservoir.update(60);
        Snapshot secondCachedSnapshot = reservoir.getSnapshot();
        assertSame(firstSnapshot, secondCachedSnapshot);
        assertEquals(10, secondCachedSnapshot.getMin());
        assertEquals(20, secondCachedSnapshot.getMax());

        time.getAndAdd(1);
        reservoir.update(70);
        reservoir.update(80);
        Snapshot firstNewSnapshot = reservoir.getSnapshot();
        assertNotSame(firstSnapshot, firstNewSnapshot);
        assertEquals(30, firstNewSnapshot.getMin());
        assertEquals(80, firstNewSnapshot.getMax());

        time.getAndAdd(1001);
        reservoir.update(90);
        reservoir.update(100);
        Snapshot secondNewSnapshot = reservoir.getSnapshot();
        assertNotSame(firstNewSnapshot, secondNewSnapshot);
        assertEquals(90, secondNewSnapshot.getMin());
        assertEquals(100, secondNewSnapshot.getMax());
    }

}
