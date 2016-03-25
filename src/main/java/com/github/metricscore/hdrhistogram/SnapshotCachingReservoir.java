package com.github.metricscore.hdrhistogram;

import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Snapshot;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class SnapshotCachingReservoir implements Reservoir {

    private final Lock lock;
    private final Reservoir target;
    private final long cachingDurationMillis;
    private final WallClock wallClock;

    Snapshot cachedSnapshot;
    long lastSnapshotTakeTimeMillis;

    public SnapshotCachingReservoir(Reservoir target, long cachingDurationMillis, WallClock wallClock) {
        this.target = target;
        this.cachingDurationMillis = cachingDurationMillis;
        this.wallClock = wallClock;
        this.lock = new ReentrantLock();
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException("You should not use this method https://github.com/dropwizard/metrics/issues/874");
    }

    @Override
    public void update(long value) {
        target.update(value);
    }

    @Override
    public Snapshot getSnapshot() {
        lock.lock();
        try {
            long nowMillis = wallClock.currentTimeMillis();
            if (cachedSnapshot == null
                    || nowMillis - lastSnapshotTakeTimeMillis >= cachingDurationMillis) {
                cachedSnapshot = target.getSnapshot();
                lastSnapshotTakeTimeMillis = nowMillis;
            }
            return cachedSnapshot;
        } finally {
            lock.unlock();
        }
    }

}
