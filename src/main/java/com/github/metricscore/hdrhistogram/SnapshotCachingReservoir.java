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
