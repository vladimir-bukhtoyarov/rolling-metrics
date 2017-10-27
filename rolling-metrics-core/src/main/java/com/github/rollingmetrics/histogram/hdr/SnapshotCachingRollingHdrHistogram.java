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

package com.github.rollingmetrics.histogram.hdr;

import com.github.rollingmetrics.util.CachingSupplier;
import com.github.rollingmetrics.util.Ticker;

import java.time.Duration;

class SnapshotCachingRollingHdrHistogram implements RollingHdrHistogram {

    private final CachingSupplier<RollingSnapshot> cachingSupplier;
    private final RollingHdrHistogram target;

    SnapshotCachingRollingHdrHistogram(RollingHdrHistogram target, Duration cachingDuration, Ticker ticker) {
        this.cachingSupplier = new CachingSupplier<>(cachingDuration, ticker, target::getSnapshot);
        this.target = target;
    }

    @Override
    public void update(long value) {
        target.update(value);
    }

    @Override
    public int getEstimatedFootprintInBytes() {
        return target.getEstimatedFootprintInBytes();
    }

    @Override
    public RollingSnapshot getSnapshot() {
        return cachingSupplier.get();
    }

}
