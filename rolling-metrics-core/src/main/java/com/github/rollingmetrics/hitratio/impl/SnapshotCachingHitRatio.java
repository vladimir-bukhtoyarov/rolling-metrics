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

package com.github.rollingmetrics.hitratio.impl;

import com.github.rollingmetrics.hitratio.HitRatio;
import com.github.rollingmetrics.retention.RetentionPolicy;
import com.github.rollingmetrics.util.CachingSupplier;

class SnapshotCachingHitRatio implements HitRatio {

    private final CachingSupplier<Double> cache;
    private final HitRatio hitRatio;

    SnapshotCachingHitRatio(RetentionPolicy retentionPolicy, HitRatio hitRatio) {
        this.hitRatio = hitRatio;
        cache = new CachingSupplier<>(retentionPolicy.getSnapshotCachingDuration(), retentionPolicy.getTicker(), hitRatio::getHitRatio);
    }

    @Override
    public void update(int hitCount, int totalCount) {
        hitRatio.update(hitCount, totalCount);
    }

    @Override
    public double getHitRatio() {
        return cache.get();
    }
}
