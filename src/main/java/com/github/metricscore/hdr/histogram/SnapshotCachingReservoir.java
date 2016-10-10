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

package com.github.metricscore.hdr.histogram;

import com.github.metricscore.hdr.Clock;
import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Snapshot;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class SnapshotCachingReservoir implements Reservoir {

    private final Lock lock;
    private final Reservoir target;
    private final long cachingDurationMillis;
    private final Clock clock;

    private Snapshot cachedSnapshot;
    private long lastSnapshotTakeTimeMillis;

    SnapshotCachingReservoir(Reservoir target, long cachingDurationMillis, Clock clock) {
        this.target = target;
        this.cachingDurationMillis = cachingDurationMillis;
        this.clock = clock;
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
            long nowMillis = clock.currentTimeMillis();
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
