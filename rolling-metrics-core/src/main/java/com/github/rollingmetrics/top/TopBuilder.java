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

import com.github.rollingmetrics.retention.*;
import com.github.rollingmetrics.top.impl.ResetByChunksTop;
import com.github.rollingmetrics.top.impl.ResetOnSnapshotConcurrentTop;
import com.github.rollingmetrics.top.impl.SnapshotCachingTop;
import com.github.rollingmetrics.top.impl.UniformTop;
import com.github.rollingmetrics.util.Ticker;
import com.github.rollingmetrics.util.ResilientExecutionUtil;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

/**
 * The builder for {@link Top}.
 *
 * <p><br> Basic examples of usage:
 * <pre> {@code
 *
 *  Top top = Top.builder(3).resetAllPositionsOnSnapshot().create();
 *  MetricSet metricSet = new TopMetricSet("my-top", top, TimeUnit.MILLISECONDS, 5);
 *  registry.registerAll(metricSet);
 * }</pre>
 *
 * @see Top
 */
public class TopBuilder {

    public static final Duration DEFAULT_SNAPSHOT_CACHING_DURATION = Duration.ofSeconds(1);

    private final RetentionPolicy retentionPolicy;
    private final TopRecorderSettings settings;

    private Duration snapshotCachingDuration = DEFAULT_SNAPSHOT_CACHING_DURATION;
    private Ticker ticker = Ticker.defaultTicker();

    /**
     * Creates new builder instance.
     *
     * @param size the count of positions for tops which will be constructed by this builder
     * @param retentionPolicy policy which responsible for decisions how long any written value should be remembered
     * @return this builder instance
     */
    TopBuilder(int size, RetentionPolicy retentionPolicy) {
        this.retentionPolicy = Objects.requireNonNull(retentionPolicy);
        if (!builders.containsKey(retentionPolicy.getClass())) {
            throw new IllegalArgumentException("Unknown retention policy " + retentionPolicy.getClass());
        }
        this.settings = new TopRecorderSettings(size);
    }

    /**
     * Constructs new {@link Top} instance
     *
     * @return new {@link Top} instance
     */
    public Top build() {
        Top top = builders.get(retentionPolicy.getClass()).create(settings, retentionPolicy, ticker);
        if (!snapshotCachingDuration.isZero()) {
            top = new SnapshotCachingTop(top, snapshotCachingDuration, ticker);
        }
        return top;
    }

    /**
     * Configures the maximum count of positions for tops which will be constructed by this builder.
     *
     * @param size the maximum count of positions
     * @return this builder instance
     */
    public TopBuilder withPositionCount(int size) {
        settings.setSize(size);
        return this;
    }

    /**
     * Configures the latency threshold. The queries having latency which shorter than threshold, will not be tracked in the top.
     * The default value is zero {@link TopRecorderSettings#DEFAULT_LATENCY_THRESHOLD}, means that all queries can be recorded independent of its latency.
     *
     * Specify this parameter when you want not to track queries which fast,
     * in other words when you want see nothing when all going well.
     * 
     * @param latencyThreshold
     * @return this builder instance
     */
    public TopBuilder withLatencyThreshold(Duration latencyThreshold) {
        settings.setLatencyThreshold(latencyThreshold);
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
     * The default value is 1000 symbol {@link TopRecorderSettings#DEFAULT_MAX_LENGTH_OF_QUERY_DESCRIPTION}.
     *
     * @param maxLengthOfQueryDescription the max length of description position int the top.
     *
     * @return this builder instance
     */
    public TopBuilder withMaxLengthOfQueryDescription(int maxLengthOfQueryDescription) {
        settings.setMaxLengthOfQueryDescription(maxLengthOfQueryDescription);
        return this;
    }

    /**
     * Replaces default ticker.
     * Most likely you should never use this method, because replacing time measuring has sense only for unit testing.
     *
     * @param ticker the abstraction over time
     *
     * @return this builder instance
     */
    public TopBuilder withTicker(Ticker ticker) {
        if (ticker == null) {
            throw new IllegalArgumentException("Ticker should not be null");
        }
        this.ticker = ticker;
        return this;
    }

    /**
     * Configures the executor which will be used for chunk rotation if top configured with {@link RetentionPolicy#resetPeriodically(Duration)} (Duration)} or {@link RetentionPolicy#resetPeriodicallyByChunks(Duration, int)}.
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
        settings.setBackgroundExecutor(backgroundExecutor);
        return this;
    }

    private static Map<Class<? extends RetentionPolicy>, TopFactory> builders = new HashMap<>();
    static {
        builders.put(UniformRetentionPolicy.class, (settings, retentionPolicy, ticker) -> new UniformTop(settings));
        builders.put(ResetOnSnapshotRetentionPolicy.class, (settings, retentionPolicy, ticker) -> new ResetOnSnapshotConcurrentTop(settings));
        builders.put(ResetPeriodicallyRetentionPolicy.class, (settings, retentionPolicy, ticker) ->
                new ResetByChunksTop(settings, (ResetPeriodicallyRetentionPolicy) retentionPolicy, ticker));
        builders.put(ResetPeriodicallyByChunksRetentionPolicy.class, (settings, retentionPolicy, ticker) ->
                new ResetByChunksTop(settings, (ResetPeriodicallyByChunksRetentionPolicy) retentionPolicy, ticker));
    }

    private interface TopFactory {

        Top create(TopRecorderSettings settings, RetentionPolicy retentionPolicy, Ticker ticker);

    }

}
