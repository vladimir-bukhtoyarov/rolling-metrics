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


import com.github.metricscore.hdr.Clock;
import com.github.metricscore.hdr.histogram.util.EmptySnapshot;
import com.github.metricscore.hdr.hitratio.HitRatioUtil;
import com.github.metricscore.hdr.top.basic.BasicQueryTop;
import com.github.metricscore.hdr.top.basic.ComposableQueryTop;
import com.github.metricscore.hdr.top.basic.QueryTopRecorder;
import org.HdrHistogram.Histogram;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;


public class ResetPeriodicalyQueryTop extends BasicQueryTop {

    static final long MIN_CHUNK_RESETTING_INTERVAL_MILLIS = 1000;

    private final QueryTopRecorder recorder;
    private final long resetIntervalMillis;
    private final Clock clock;
    private final AtomicLong nextResetTimeMillisRef;
    private final ComposableQueryTop uniformQueryTop;

    private ComposableQueryTop intervalQueryTop;

    public ResetPeriodicalyQueryTop(int size, Duration slowQueryThreshold, Duration resetInterval, Clock clock) {
        super(size, slowQueryThreshold);
        this.resetIntervalMillis = resetInterval.toMillis();
        if (resetInterval.toMillis() < MIN_CHUNK_RESETTING_INTERVAL_MILLIS) {
            throw new IllegalArgumentException("resetInterval should be >= " + MIN_CHUNK_RESETTING_INTERVAL_MILLIS + " millis");
        }

        this.clock = Objects.requireNonNull(clock);
        this.recorder = new QueryTopRecorder(size, slowQueryThreshold);
        this.intervalQueryTop = recorder.getIntervalQueryTop();
        this.nextResetTimeMillisRef = new AtomicLong(clock.currentTimeMillis() + resetIntervalMillis);
        this.uniformQueryTop = ComposableQueryTop.create(size, slowQueryThreshold);
    }

    @Override
    synchronized public List<LatencyWithDescription> getDescendingRaiting() {
        resetIfNeeded();
        intervalQueryTop = recorder.getIntervalQueryTop();
        uniformQueryTop.add(intervalQueryTop);
        return uniformQueryTop.getDescendingRaiting();
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
                nextResetTimeMillisRef.set(currentTimeMillis + resetIntervalMillis);
            }
        }
    }

}
