/*
 *    Copyright 2017 Vladimir Bukhtoyarov
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package com.github.rollingmetrics.counter.impl;

import com.github.rollingmetrics.counter.WindowCounter;
import com.github.rollingmetrics.retention.RetentionPolicy;
import com.github.rollingmetrics.util.CachingSupplier;

class SnapshotCachingWindowCounter implements WindowCounter {

    private final CachingSupplier<Long> cachingSupplier;
    private final WindowCounter counter;

    SnapshotCachingWindowCounter(RetentionPolicy retentionPolicy, WindowCounter counter) {
        this.counter = counter;
        cachingSupplier = new CachingSupplier<>(retentionPolicy.getSnapshotCachingDuration(), retentionPolicy.getTicker(), counter::getSum);
    }

    @Override
    public void add(long delta) {
        counter.add(delta);
    }

    @Override
    public long getSum() {
        return cachingSupplier.get();
    }

}
