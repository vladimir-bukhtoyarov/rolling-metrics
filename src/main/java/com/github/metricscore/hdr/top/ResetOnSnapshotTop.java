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

class ResetOnSnapshotTop extends BasicTop {

    private final TopRecorder recorder;
    private ComposableTop intervalQueryTop;

    ResetOnSnapshotTop(int size, Duration slowQueryThreshold) {
        super(size, slowQueryThreshold);
        ComposableTop active = size == 1? new ConcurrentSingletonTop(slowQueryThreshold): new ConcurrentMultipositionTop(size, slowQueryThreshold);
        this.recorder = new TopRecorder(active);
        this.intervalQueryTop = recorder.getIntervalQueryTop();
    }

    @Override
    synchronized public List<LatencyWithDescription> getPositionsInDescendingOrder() {
        intervalQueryTop = recorder.getIntervalQueryTop(intervalQueryTop);
        return intervalQueryTop.getPositionsInDescendingOrder();
    }

    @Override
    protected void updateImpl(long latencyTime, TimeUnit latencyUnit, Supplier<String> descriptionSupplier, long latencyNanos) {
        recorder.update(latencyTime, latencyUnit, descriptionSupplier);
    }

}
