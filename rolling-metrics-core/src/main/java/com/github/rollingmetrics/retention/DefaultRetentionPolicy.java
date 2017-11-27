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

package com.github.rollingmetrics.retention;

import com.github.rollingmetrics.counter.WindowCounter;
import com.github.rollingmetrics.counter.impl.WindowCounterUtil;
import com.github.rollingmetrics.histogram.hdr.RollingHdrHistogramBuilder;
import com.github.rollingmetrics.histogram.hdr.impl.DefaultRollingHdrHistogramBuilder;
import com.github.rollingmetrics.hitratio.HitRatio;
import com.github.rollingmetrics.hitratio.impl.HitRatioUtil;
import com.github.rollingmetrics.top.TopBuilder;
import com.github.rollingmetrics.top.impl.DefaultTopBuilder;
import com.github.rollingmetrics.util.ResilientExecutionUtil;
import com.github.rollingmetrics.util.Ticker;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;

public abstract class DefaultRetentionPolicy implements RetentionPolicy {

    private Duration snapshotCachingDuration = Duration.ZERO;
    private Ticker ticker = Ticker.defaultTicker();
    private Supplier<Executor> executorSupplier = () -> ResilientExecutionUtil.getInstance().getBackgroundExecutor();

    /**
     * Configures the executor which will be used for chunk rotation if histogram configured with {@link RetentionPolicy#resetPeriodically(Duration)} (Duration)} or {@link RetentionPolicy#resetPeriodicallyByChunks(Duration, int)}.
     *
     * <p>
     * Normally you should not use this method because of default executor provided by {@link ResilientExecutionUtil#getBackgroundExecutor()} is quietly enough for mostly use cases.
     * </p>
     *
     * <p>
     * You can use this method for example inside JEE environments with enabled SecurityManager,
     * in case of {@link ResilientExecutionUtil#setThreadFactory(ThreadFactory)} is not enough to meat security rules.
     * </p>
     *
     * @return TODO this builder instance
     */
    public DefaultRetentionPolicy withBackgroundExecutor(Executor backgroundExecutor) {
        if (backgroundExecutor == null) {
            throw new IllegalArgumentException("backgroundExecutor must not be null");
        }
        this.executorSupplier = () -> backgroundExecutor;
        return this;
    }

    /**
     * Replaces default ticker.
     * Most likely you should never use this method, because replacing time measuring has sense only for unit testing.
     *
     * @param ticker the abstraction over time
     *
     * @return TODO this builder instance
     */
    public DefaultRetentionPolicy withTicker(Ticker ticker) {
        if (ticker == null) {
            throw new IllegalArgumentException("Ticker should not be null");
        }
        this.ticker = ticker;
        return this;
    }

    /**
     * Configures the period for which taken snapshot will be cached.
     *
     * @param duration the period for which taken snapshot will be cached, should be a positive duration.
     * @return TODO this builder instance
     */
    public DefaultRetentionPolicy withSnapshotCachingDuration(Duration duration) {
        if (duration == null) {
            throw new IllegalArgumentException("snapshotCachingDuration must not be null");
        }
        if (duration.isNegative()) {
            throw new IllegalArgumentException(duration + " is negative");
        }
        this.snapshotCachingDuration = duration;
        return this;
    }

    @Override
    public Executor getExecutor() {
        return executorSupplier.get();
    }

    @Override
    public Ticker getTicker() {
        return ticker;
    }

    @Override
    public Duration getSnapshotCachingDuration() {
        return snapshotCachingDuration;
    }

    @Override
    public WindowCounter newCounter() {
        return WindowCounterUtil.build(this);
    }

    @Override
    public HitRatio newHitRatio() {
        return HitRatioUtil.build(this);
    }

    @Override
    public RollingHdrHistogramBuilder newRollingHdrHistogramBuilder() {
        return new DefaultRollingHdrHistogramBuilder(this);
    }

    @Override
    public TopBuilder newTopBuilder(int size) {
        return new DefaultTopBuilder(size,this);
    }

}
