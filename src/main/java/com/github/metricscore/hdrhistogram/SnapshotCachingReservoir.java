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
    private final MutableState state;

    private static final class MutableState {
        Snapshot cachedSnapshot = null;
        long lastSnapshotTakeTimeMillis = Long.MIN_VALUE;
    }

    public SnapshotCachingReservoir(Reservoir target, long cachingDurationMillis, WallClock wallClock) {
        this.target = target;
        this.cachingDurationMillis = cachingDurationMillis;
        this.wallClock = wallClock;
        this.lock = new ReentrantLock();
        this.state = new MutableState();
    }

    @Override
    public int size() {
        return target.size();
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
            if (state.cachedSnapshot == null
                    || state.lastSnapshotTakeTimeMillis == Long.MIN_VALUE
                    || nowMillis - state.lastSnapshotTakeTimeMillis >= cachingDurationMillis) {
                state.cachedSnapshot = target.getSnapshot();
                state.lastSnapshotTakeTimeMillis = nowMillis;
            }
            return state.cachedSnapshot;
        } finally {
            lock.unlock();
        }
    }

}
