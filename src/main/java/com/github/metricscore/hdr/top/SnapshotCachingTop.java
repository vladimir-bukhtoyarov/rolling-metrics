/*
 *    Copyright 2016 Vladimir Bukhtoyarov
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

package com.github.metricscore.hdr.top;


import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class SnapshotCachingTop implements Top {

    private final Top target;
    private final long cachingDurationMillis;

    public SnapshotCachingTop(Top target, long cachingDurationMillis) {
        this.target = target;
        this.cachingDurationMillis = cachingDurationMillis;
    }

    @Override
    public void update(long latencyTime, TimeUnit latencyUnit, Supplier<String> descriptionSupplier) {
        target.update(latencyTime, latencyUnit, descriptionSupplier);
    }

    @Override
    public List<LatencyWithDescription> getPositionsInDescendingOrder() {
        // TODO
        return null;
    }

    @Override
    public int getPositionCount() {
        return target.getPositionCount();
    }

    @Override
    public long getSlowQueryThresholdNanos() {
        return target.getSlowQueryThresholdNanos();
    }

}
