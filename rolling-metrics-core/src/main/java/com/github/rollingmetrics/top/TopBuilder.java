/*
 *
 *  Copyright 2017 Vladimir Bukhtoyarov
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.github.rollingmetrics.top;

import com.github.rollingmetrics.top.impl.ResetByChunksTop;
import com.github.rollingmetrics.top.impl.ResetOnSnapshotConcurrentTop;
import com.github.rollingmetrics.top.impl.SnapshotCachingTop;
import com.github.rollingmetrics.top.impl.UniformTop;
import com.github.rollingmetrics.util.Clock;
import com.github.rollingmetrics.util.ResilientExecutionUtil;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

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
     * Specifies the max length of description position int the top. The characters upper {@code maxLengthOfQueryDescription} limit will be truncated
     *
     * <p>
     * The default value is 1000 symbol {@link #DEFAULT_MAX_LENGTH_OF_QUERY_DESCRIPTION}.
     *
     * @param maxLengthOfQueryDescription the max length of description position int the top.
     *
     * @return this builder instance
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
     * Replaces default clock.
     * Most likely you should never use this method, because replacing time measuring has sense only for unit testing.
     *
     * @param clock the abstraction over time
     *
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
     * Configures the executor which will be used if any of {@link #resetAllPositionsPeriodically(Duration)} (Duration)} or {@link #resetPositionsPeriodicallyByChunks(Duration, int)} (Duration, int)} (Duration)}.
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
     * Top configured with this strategy will store all values since the top was created.
     *
     * <p>This is default strategy for {@link TopBuilder}.
     * This strategy is useless for long running applications, because very slow queries happen in the past
     * will not provide chances to fresh queries to take place in the top.
     * So, it is strongly recommended to switch eviction strategy to one of:
     * <ul>
     *     <li>{@link #resetAllPositionsPeriodically(Duration)}</li>
     *     <li>{@link #resetPositionsPeriodicallyByChunks(Duration, int)}</li>
     *     <li>{@link #resetAllPositionsOnSnapshot()}</li>
     * </ul>
     *
     * @return this builder instance
     *
     * @see #resetPositionsPeriodicallyByChunks(Duration, int)
     * @see #resetAllPositionsPeriodically(Duration)
     * @see #resetAllPositionsOnSnapshot()
     */
    public TopBuilder neverResetPositions() {
        this.factory = TopFactory.UNIFORM;
        return this;
    }

    /**
     * Top configured with this strategy will be cleared each time when {@link Top#getPositionsInDescendingOrder()} invoked.
     *
     * @return this builder instance
     * @see #resetPositionsPeriodicallyByChunks(Duration, int)
     * @see #resetAllPositionsPeriodically(Duration)
     */
    public TopBuilder resetAllPositionsOnSnapshot() {
        this.factory = TopFactory.RESET_ON_SNAPSHOT;
        return this;
    }

    /**
     * Top configured with this strategy will be cleared at all after each {@code intervalBetweenResetting} elapsed.
     *
     * <p>
     *     If You use this strategy inside JEE environment,
     *     then it would be better to call {@code ResilientExecutionUtil.getInstance().shutdownBackgroundExecutor()}
     *     once in application shutdown listener,
     *     in order to avoid leaking reference to classloader through the thread which this library creates for resetting of top in background.
     * </p>
     *
     * @param intervalBetweenResetting specifies how often need to reset the top
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
     * Top configured with this strategy will be divided to <tt>numberChunks</tt> parts,
     * and one chunk will be cleared after each <tt>rollingTimeWindow / numberChunks</tt> elapsed.
     * This strategy is more smoothly then <tt>resetAllPositionsPeriodically</tt> because top never zeroed at whole,
     * so user experience provided by <tt>resetPositionsPeriodicallyByChunks</tt> should look more pretty.
     * <p>
     * The value recorded to top will take affect at least <tt>rollingTimeWindow</tt> and at most <tt>rollingTimeWindow *(1 + 1/numberChunks)</tt> time,
     * for example when you configure <tt>rollingTimeWindow=60 seconds and numberChunks=6</tt> then each value recorded to top will be stored at <tt>60-70 seconds</tt>
     * </p>
     *
     * <p>
     *     If You use this strategy inside JEE environment,
     *     then it would be better to call {@code ResilientExecutionUtil.getInstance().shutdownBackgroundExecutor()}
     *     once in application shutdown listener,
     *     in order to avoid leaking reference to classloader through the thread which this library creates for rotation of top in background.
     * </p>
     *
     * @param rollingTimeWindow the total rolling time window, any value recorded to top will not be evicted from it at least <tt>rollingTimeWindow</tt>
     * @param numberChunks specifies number of chunks by which the top will be slitted
     * @return this builder instance
     */
    public TopBuilder resetPositionsPeriodicallyByChunks(Duration rollingTimeWindow, int numberChunks) {
        if (numberChunks > MAX_CHUNKS) {
            throw new IllegalArgumentException("numberChunks should be <= " + MAX_CHUNKS);
        }
        if (numberChunks < 2) {
            throw new IllegalArgumentException("numberChunks should be >= 1");
        }
        if (rollingTimeWindow == null) {
            throw new IllegalArgumentException("rollingTimeWindow should not be null");
        }
        if (rollingTimeWindow.isNegative()) {
            throw new IllegalArgumentException("rollingTimeWindow should not be negative");
        }

        long intervalBetweenResettingMillis = rollingTimeWindow.toMillis() / numberChunks;
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
