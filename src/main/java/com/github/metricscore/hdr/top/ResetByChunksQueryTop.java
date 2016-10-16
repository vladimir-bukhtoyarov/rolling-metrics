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
import com.github.metricscore.hdr.top.basic.BasicQueryTop;
import com.github.metricscore.hdr.top.basic.ComposableQueryTop;
import com.github.metricscore.hdr.top.basic.QueryTopRecorder;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;


class ResetByChunksQueryTop extends BasicQueryTop {

    static final long MIN_CHUNK_RESETTING_INTERVAL_MILLIS = 1000;
    static final int MAX_CHUNKS = 25;

    private final QueryTopRecorder recorder;
    private final long intervalBetweenResettingMillis;
    private final Clock clock;
    private final AtomicLong nextResetTimeMillisRef;
    private final ComposableQueryTop uniformQueryTop;

    private ComposableQueryTop intervalQueryTop;

    ResetByChunksQueryTop(int size, Duration slowQueryThreshold, Duration rollingWindow, int numberChunks, Clock clock) {
        super(size, slowQueryThreshold);
        if (numberChunks > MAX_CHUNKS) {
            throw new IllegalArgumentException("numberChunks should be <= " + MAX_CHUNKS);
        }
        if (numberChunks < 1) {
            throw new IllegalArgumentException("numberChunks should be >= 1");
        }

        this.intervalBetweenResettingMillis = rollingWindow.toMillis();
        if (intervalBetweenResettingMillis < MIN_CHUNK_RESETTING_INTERVAL_MILLIS) {
            String msg = "interval between resetting one chunk should be >= " + MIN_CHUNK_RESETTING_INTERVAL_MILLIS + " millis";
            throw new IllegalArgumentException(msg);
        }

        this.clock = Objects.requireNonNull(clock);
        this.recorder = new QueryTopRecorder(size, slowQueryThreshold);
        this.intervalQueryTop = recorder.getIntervalQueryTop();
        this.nextResetTimeMillisRef = new AtomicLong(clock.currentTimeMillis() + intervalBetweenResettingMillis);
        this.uniformQueryTop = ComposableQueryTop.create(size, slowQueryThreshold);
    }

    @Override
    synchronized public List<LatencyWithDescription> getDescendingRating() {
        resetIfNeeded();
        intervalQueryTop = recorder.getIntervalQueryTop();
        uniformQueryTop.add(intervalQueryTop);
        return uniformQueryTop.getDescendingRating();
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
