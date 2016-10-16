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


import com.github.metricscore.hdr.top.basic.*;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

class ResetOnSnapshotQueryTop extends BasicQueryTop {

    private final QueryTopRecorder recorder;
    private ComposableQueryTop intervalQueryTop;

    ResetOnSnapshotQueryTop(int size, Duration slowQueryThreshold) {
        super(size, slowQueryThreshold);
        ComposableQueryTop active = size == 1? new SingletonTop(slowQueryThreshold): new ConcurrentQueryTop(size, slowQueryThreshold);
        this.recorder = new QueryTopRecorder(active);
        this.intervalQueryTop = recorder.getIntervalQueryTop();
    }

    @Override
    synchronized public List<LatencyWithDescription> getDescendingRating() {
        intervalQueryTop = recorder.getIntervalQueryTop(intervalQueryTop);
        return intervalQueryTop.getDescendingRating();
    }

    @Override
    protected void updateImpl(long latencyTime, TimeUnit latencyUnit, Supplier<String> descriptionSupplier, long latencyNanos) {
        recorder.update(latencyTime, latencyUnit, descriptionSupplier);
    }

}
