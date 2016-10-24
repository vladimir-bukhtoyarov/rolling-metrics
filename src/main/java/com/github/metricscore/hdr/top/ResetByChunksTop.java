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


import com.github.metricscore.hdr.util.Clock;
import com.github.metricscore.hdr.top.basic.BasicTop;
import com.github.metricscore.hdr.top.basic.ComposableTop;
import com.github.metricscore.hdr.top.basic.TopRecorder;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;


class ResetByChunksTop extends BasicTop {

    private final TopRecorder recorder;
    private final long intervalBetweenResettingMillis;
    private final Clock clock;
    private final AtomicLong nextResetTimeMillisRef;
    private final ComposableTop uniformQueryTop;

    private ComposableTop intervalQueryTop;

    ResetByChunksTop(int size, Duration slowQueryThreshold, long intervalBetweenResettingMillis, int numberChunks, Clock clock, Executor backgroundExecutor) {
        super(size, slowQueryThreshold);
        this.intervalBetweenResettingMillis = intervalBetweenResettingMillis;

        this.clock = Objects.requireNonNull(clock);
        this.recorder = null;//new QueryTopRecorder(size, slowQueryThreshold);
        this.intervalQueryTop = recorder.getIntervalQueryTop();
        this.nextResetTimeMillisRef = new AtomicLong(clock.currentTimeMillis() + intervalBetweenResettingMillis);
        this.uniformQueryTop = null;//ComposableQueryTop.create(size, slowQueryThreshold);
    }

    @Override
    synchronized public List<LatencyWithDescription> getPositionsInDescendingOrder() {
        resetIfNeeded();
        intervalQueryTop = recorder.getIntervalQueryTop();
        uniformQueryTop.add(intervalQueryTop);
        return uniformQueryTop.getPositionsInDescendingOrder();
    }

    @Override
    protected void updateImpl(long latencyTime, TimeUnit latencyUnit, Supplier<String> descriptionSupplier, long latencyNanos) {
        recorder.update(latencyTime, latencyUnit, descriptionSupplier);
    }

    private void resetIfNeeded() {
        long nextResetTimeMillis = nextResetTimeMillisRef.get();
        long currentTimeMillis = clock.currentTimeMillis();
        if (currentTimeMillis >= nextResetTimeMillis) {
            if (nextResetTimeMillisRef.compareAndSet(nextResetTimeMillis, Long.MAX_VALUE)) {
                recorder.reset();
                uniformQueryTop.reset();
                nextResetTimeMillisRef.set(currentTimeMillis + intervalBetweenResettingMillis);
            }
        }
    }

}
