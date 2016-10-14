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


import com.github.metricscore.hdr.top.basic.BasicQueryTop;
import com.github.metricscore.hdr.top.basic.QueryTopRecorder;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Created by vladimir.bukhtoyarov on 14.10.2016.
 */
public class ResetOnSnapshotQueryTop extends BasicQueryTop {

    private final QueryTopRecorder recorder;
    private QueryTop intervalQueryTop;

    ResetOnSnapshotQueryTop(int size, Duration slowQueryThreshold) {
        super(size, slowQueryThreshold);
        this.recorder = new QueryTopRecorder(size, slowQueryThreshold);
    }

    @Override
    synchronized public List<LatencyWithDescription> getDescendingRaiting() {
        intervalQueryTop = recorder.getIntervalQueryTop();
        return intervalQueryTop.getDescendingRaiting();
    }

    @Override
    protected void updateImpl(long latencyTime, TimeUnit latencyUnit, Supplier<String> descriptionSupplier, long latencyNanos) {
        recorder.update(latencyTime, latencyUnit, descriptionSupplier);
    }

    @Override
    public void reset() {
        recorder.reset();
    }

}
