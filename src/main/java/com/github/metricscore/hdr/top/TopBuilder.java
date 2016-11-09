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

import com.github.metricscore.hdr.top.impl.ResetByChunksTop;
import com.github.metricscore.hdr.top.impl.ResetOnSnapshotConcurrentTop;
import com.github.metricscore.hdr.top.impl.SnapshotCachingTop;
import com.github.metricscore.hdr.top.impl.UniformTop;
import com.github.metricscore.hdr.util.Clock;
import com.github.metricscore.hdr.util.ResilientExecutionUtil;

import java.time.Duration;
import java.util.concurrent.Executor;

/**
 * The builder for {@link Top}.
 *
 * <p><br> Basic examples of usage:
 * <pre> {@code
 *
 *  Top top = Top.builder(3).resetAllPositionsOnSnapshot().build();
 *  MetricSet metricSet = new TopMetricSet("my-top", top, TimeUnit.MILLISECONDS, 5);
 *  registry.registerAll(metricSet);
 * }</pre>
 *
 * <p>
 * The responsibility of builder is only construction of {@link Top}, you should use {@link TopMetricSet} to integrate constructed {@link Top} into {@link com.codahale.metrics.MetricRegistry}
 *
 * @see Top
 * @see TopMetricSet
 */
public class TopBuilder {

    public static final int MAX_POSITION_COUNT = 1000;
    public static final long MIN_CHUNK_RESETTING_INTERVAL_MILLIS = 1000;
    public static final int MAX_CHUNKS = 25;
    public static final int MIN_LENGTH_OF_QUERY_DESCRIPTION = 10;
    public static final int DEFAULT_MAX_LENGTH_OF_QUERY_DESCRIPTION = 1000;

    public static final Duration DEFAULT_LATENCY_THRESHOLD = Duration.ZERO;
    public static final Duration DEFAULT_SNAPSHOT_CACHING_DURATION = Duration.ofSeconds(1);

    private static final Executor DEFAULT_BACKGROUND_EXECUTOR = null;
    private static final TopFactory DEFAULT_TOP_FACTORY = TopFactory.UNIFORM;

    private int size;
    private Duration latencyThreshold;
    private Duration snapshotCachingDuration;
    private int maxDescriptionLength;
    private Clock clock;
    private Executor backgroundExecutor;
    private TopFactory factory;

    private TopBuilder(int size, Duration latencyThreshold, Duration snapshotCachingDuration, int maxDescriptionLength, Clock clock, Executor backgroundExecutor, TopFactory factory) {
        this.size = size;
        this.latencyThreshold = latencyThreshold;
        this.snapshotCachingDuration = snapshotCachingDuration;
        this.maxDescriptionLength = maxDescriptionLength;
        this.clock = clock;
        this.backgroundExecutor = backgroundExecutor;
        this.factory = factory;
    }

    /**
     * Constructs new {@link Top} instance
     *
     * @return new {@link Top} instance
     */
    public Top build() {
        Top top = factory.create(size, latencyThreshold, maxDescriptionLength, clock);
        if (!snapshotCachingDuration.isZero()) {
            top = new SnapshotCachingTop(top, snapshotCachingDuration.toMillis(), clock);
        }
        return top;
    }

    /**
     * Creates new builder instance.
     *
     * @param size the count of positions for tops which will be constructed by this builder
     * @return this builder instance
     */
    public static TopBuilder newBuilder(int size) {
        validateSize(size);
        return new TopBuilder(size, DEFAULT_LATENCY_THRESHOLD, DEFAULT_SNAPSHOT_CACHING_DURATION, DEFAULT_MAX_LENGTH_OF_QUERY_DESCRIPTION, Clock.defaultClock(), DEFAULT_BACKGROUND_EXECUTOR, DEFAULT_TOP_FACTORY);
    }

    /**
     * Configures the maximum count of positions for tops which will be constructed by this builder.
     *
     * @param size the maximum count of positions
     * @return this builder instance
     */
    public TopBuilder withPositionCount(int size) {
        validateSize(size);
        this.size = size;
        return this;
    }

    /**
     * Configures the latency threshold. The queries having latency which shorter than threshold, will not be tracked in the top.
     * The default value is zero {@link #DEFAULT_LATENCY_THRESHOLD}, means that all queries can be recorded independent of its latency.
     *
     * Specify this parameter when you want not to track queries which fast,
     * in other words when you want see nothing when all going well.
     * 
     * @param latencyThreshold
     * @return this builder instance
     */
    public TopBuilder withLatencyThreshold(Duration latencyThreshold) {
        if (latencyThreshold == null) {
            throw new IllegalArgumentException("latencyThreshold should not be null");
        }
        if (latencyThreshold.isNegative()) {
            throw new IllegalArgumentException("latencyThreshold should not be negative");
        }
        this.latencyThreshold = latencyThreshold;
        return this;
    }

    /**
     * Configures the duration for caching the results of invocation of {@link Top#getPositionsInDescendingOrder()}.
     * Currently the caching is only one way to solve <a href="https://github.com/dropwizard/metrics/issues/1016">atomicity read problem</a>.
     * The default value is one second {@link #DEFAULT_SNAPSHOT_CACHING_DURATION}.
     * You can specify zero duration to discard caching at all, but theoretically,
     * you should not disable cache until Dropwizard-Metrics reporting pipeline will be rewritten <a href="https://github.com/marshallpierce/metrics-thoughts">in scope of v-4-0</a>
     *
     * @param snapshotCachingDuration
     * @return this builder instance
     */
    public TopBuilder withSnapshotCachingDuration(Duration snapshotCachingDuration) {
        if (snapshotCachingDuration == null) {
            throw new IllegalArgumentException("snapshotCachingDuration should not be null");
        }
        if (snapshotCachingDuration.isNegative()) {
            throw new IllegalArgumentException("snapshotCachingDuration can not be negative");
        }
        this.snapshotCachingDuration = snapshotCachingDuration;
        return this;
    }

    /**
     *
     * @param maxLengthOfQueryDescription
     * @return
     */
    public TopBuilder withMaxLengthOfQueryDescription(int maxLengthOfQueryDescription) {
        if (maxLengthOfQueryDescription < MIN_LENGTH_OF_QUERY_DESCRIPTION) {
            String msg = "The requested maxDescriptionLength=" + maxLengthOfQueryDescription + " is wrong " +
                    "because of maxDescriptionLength should be >=" + MIN_LENGTH_OF_QUERY_DESCRIPTION + "." +
                    "How do you plan to distinguish one query from another with so short description?";
            throw new IllegalArgumentException(msg);
        }
        this.maxDescriptionLength = maxLengthOfQueryDescription;
        return this;
    }

    /**
     *
     * @param clock
     * @return this builder instance
     */
    public TopBuilder withClock(Clock clock) {
        if (clock == null) {
            throw new IllegalArgumentException("Clock should not be null");
        }
        this.clock = clock;
        return this;
    }

    /**
     *
     * @param backgroundExecutor
     * @return this builder instance
     */
    public TopBuilder withBackgroundExecutor(Executor backgroundExecutor) {
        if (backgroundExecutor == null) {
            throw new IllegalArgumentException("Clock should not be null");
        }
        this.backgroundExecutor = backgroundExecutor;
        return this;
    }

    /**
     *
     * @return this builder instance
     */
    public TopBuilder neverResetPositions() {
        this.factory = TopFactory.UNIFORM;
        return this;
    }

    /**
     *
     * @return this builder instance
     */
    public TopBuilder resetAllPositionsOnSnapshot() {
        this.factory = TopFactory.RESET_ON_SNAPSHOT;
        return this;
    }

    /**
     *
     * @param intervalBetweenResetting
     * @return this builder instance
     */
    public TopBuilder resetAllPositionsPeriodically(Duration intervalBetweenResetting) {
        if (intervalBetweenResetting == null) {
            throw new IllegalArgumentException("intervalBetweenResetting should not be null");
        }
        if (intervalBetweenResetting.isNegative()) {
            throw new IllegalArgumentException("intervalBetweenResetting should not be negative");
        }
        long intervalBetweenResettingMillis = intervalBetweenResetting.toMillis();
        if (intervalBetweenResettingMillis < MIN_CHUNK_RESETTING_INTERVAL_MILLIS) {
            String msg = "interval between resetting one chunk should be >= " + MIN_CHUNK_RESETTING_INTERVAL_MILLIS + " millis";
            throw new IllegalArgumentException(msg);
        }
        this.factory = resetByChunks(intervalBetweenResettingMillis, 0);
        return this;
    }

    /**
     *
     * @param rollingWindow
     * @param numberChunks
     * @return this builder instance
     */
    public TopBuilder resetAllPositionsPeriodicallyByChunks(Duration rollingWindow, int numberChunks) {
        if (numberChunks > MAX_CHUNKS) {
            throw new IllegalArgumentException("numberChunks should be <= " + MAX_CHUNKS);
        }
        if (numberChunks < 2) {
            throw new IllegalArgumentException("numberChunks should be >= 1");
        }
        if (rollingWindow == null) {
            throw new IllegalArgumentException("rollingWindow should not be null");
        }
        if (rollingWindow.isNegative()) {
            throw new IllegalArgumentException("rollingWindow should not be negative");
        }

        long intervalBetweenResettingMillis = rollingWindow.toMillis() / numberChunks;
        if (intervalBetweenResettingMillis < MIN_CHUNK_RESETTING_INTERVAL_MILLIS) {
            String msg = "interval between resetting one chunk should be >= " + MIN_CHUNK_RESETTING_INTERVAL_MILLIS + " millis";
            throw new IllegalArgumentException(msg);
        }
        this.factory = resetByChunks(intervalBetweenResettingMillis, numberChunks);
        return this;
    }


    private interface TopFactory {

        Top create(int size, Duration latencyThreshold, int maxDescriptionLength, Clock clock);

        TopFactory UNIFORM = new TopFactory() {
            @Override
            public Top create(int size, Duration latencyThreshold, int maxDescriptionLength, Clock clock) {
                return new UniformTop(size, latencyThreshold.toNanos(), maxDescriptionLength);
            }
        };

        TopFactory RESET_ON_SNAPSHOT = new TopFactory() {
            @Override
            public Top create(int size, Duration latencyThreshold, int maxDescriptionLength, Clock clock) {
                return new ResetOnSnapshotConcurrentTop(size, latencyThreshold.toNanos(), maxDescriptionLength);
            }
        };

    }

    private static void validateSize(int size) {
        if (size < 1) {
            throw new IllegalArgumentException("size should be >=1");
        }
        if (size > MAX_POSITION_COUNT) {
            throw new IllegalArgumentException("size should be <= " + MAX_POSITION_COUNT);
        }
    }

    private TopFactory resetByChunks(final long intervalBetweenResettingMillis, int numberOfHistoryChunks) {
        return new TopFactory() {
            @Override
            public Top create(int size, Duration latencyThreshold, int maxDescriptionLength, Clock clock) {
                return new ResetByChunksTop(size, latencyThreshold.toNanos(), maxDescriptionLength, intervalBetweenResettingMillis, numberOfHistoryChunks, clock, getExecutor());
            }
        };
    }

    private Executor getExecutor() {
        return backgroundExecutor != null ? backgroundExecutor : ResilientExecutionUtil.getInstance().getBackgroundExecutor();
    }

}
