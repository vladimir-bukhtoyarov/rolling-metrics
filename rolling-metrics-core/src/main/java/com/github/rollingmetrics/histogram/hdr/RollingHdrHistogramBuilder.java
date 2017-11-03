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

package com.github.rollingmetrics.histogram.hdr;

import com.github.rollingmetrics.histogram.OverflowResolver;
import com.github.rollingmetrics.histogram.hdr.impl.ResetByChunksRollingHdrHistogramImpl;
import com.github.rollingmetrics.histogram.hdr.impl.ResetOnSnapshotRollingHdrHistogramImpl;
import com.github.rollingmetrics.histogram.hdr.impl.UniformRollingHdrHistogramImpl;
import com.github.rollingmetrics.retention.*;
import com.github.rollingmetrics.util.ResilientExecutionUtil;
import com.github.rollingmetrics.util.Ticker;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

/**
 * The builder for creation and registration histograms, timers and reservoirs.
 *
 * <p><br> Basic examples of usage:
 * <pre><code>
 *         TODO
 *         RollingHdrHistogramBuilder builder = RollingHdrHistogramBuilder();
 *
 *         // create and register timer
 *         Timer timer1 = builder.buildAndRegisterTimer(registry, "my-timer-1");
 *
 *         // create and register timer in another way
 *         Timer timer2 = builder.buildTimer();
 *         registry.register(timer2, "my-timer-2");
 *
 *         // create and register histogram
 *         Histogram histogram1 = builder.buildAndRegisterHistogram(registry, "my-histogram-1");
 *
 *         // create and register histogram in another way
 *         Histogram histogram2 = builder.buildHistogram();
 *         registry.register(histogram2, "my-histogram-2");
 *     </code>
 * </pre>
 * <p>
 * In order to be sure that Reservoir with provided settings does not consume too much memory you can use {@link #getEstimatedFootprintInBytes()} method which returns conservatively high estimation of the Reservoir's total footprint in bytes:
 * <pre><code>
 *         RollingHdrHistogramBuilder builder = new RollingHdrHistogramBuilder().withSignificantDigits(3);
 *         System.out.println(builder.getEstimatedFootprintInBytes());
 * </code>
 * </pre>
 *
 * @see org.HdrHistogram.Histogram
 */
public class RollingHdrHistogramBuilder {

    /**
     * Configures the number of significant decimal digits to which the histogram will maintain value resolution and separation.
     * <p>
     * Pay attention that numberOfSignificantValueDigits is major setting which affects the memory footprint, higher value will lead to higher memory consumption,
     * use {@link #getEstimatedFootprintInBytes()} to be sure that Reservoir with provided settings does not consume too much memory.
     * </p>
     * <p>
     * If numberOfSignificantValueDigits is not configured then default value <tt>2</tt> will be applied.
     * </p>
     *
     * @param numberOfSignificantValueDigits The number of significant decimal digits. Must be a non-negative integer between 0 and 5.
     * @return this builder instance
     * @see org.HdrHistogram.AbstractHistogram#AbstractHistogram(int)
     */
    public RollingHdrHistogramBuilder withSignificantDigits(int numberOfSignificantValueDigits) {
        recorderSettings.setSignificantDigits(numberOfSignificantValueDigits);
        return this;
    }

    /**
     * Configures the lowest value that can be discerned. Providing a lowestDiscernibleValue is useful is situations where the units used
     * for the histogram's values are much smaller that the minimal accuracy required. E.g. when tracking
     * time values stated in nanosecond units, where the minimal accuracy required is a microsecond, the
     * proper value for lowestDiscernibleValue would be 1000.
     * <p>
     * If you configured lowestDiscernibleValue then highestTrackableValue must be configured via {@link #withHighestTrackableValue(long, OverflowResolver)}
     * otherwise IllegalStateException will be thrown during reservoir construction.
     * </p>
     * <p>
     * There is no default value for lowestDiscernibleValue, when it is not specified then it will not be applied.
     * </p>
     *
     * @param lowestDiscernibleValue The lowest value that can be discerned (distinguished from 0) by the histogram.
     *                               Must be a positive integer that is {@literal >=} 1. May be internally rounded
     *                               down to nearest power of 2.
     * @return this builder instance
     * @see org.HdrHistogram.AbstractHistogram#AbstractHistogram(long, long, int)
     */
    public RollingHdrHistogramBuilder withLowestDiscernibleValue(long lowestDiscernibleValue) {
        recorderSettings.setLowestDiscernibleValue(lowestDiscernibleValue);
        return this;
    }

    /**
     * Configures the highest value to be tracked by the histogram.
     *
     * @param highestTrackableValue highest value to be tracked by the histogram. Must be a positive integer that is {@literal >=} (2 * lowestDiscernibleValue)
     * @param overflowResolver      specifies behavior which should be applied when writing to reservoir value which greater than highestTrackableValue
     * @return this builder instance
     */
    public RollingHdrHistogramBuilder withHighestTrackableValue(long highestTrackableValue, OverflowResolver overflowResolver) {
        recorderSettings.setHighestTrackableValue(highestTrackableValue, overflowResolver);
        return this;
    }

    /**
     * When this setting is configured then it will be used to compensate for the loss of sampled values when a recorded value is larger than the expected interval between value samples,
     * Histogram will auto-generate an additional series of decreasingly-smaller (down to the expectedIntervalBetweenValueSamples) value records.
     *
     * <p>
     * <strong>WARNING:</strong> You should not use this method for monitoring your application in the production,
     * its designed to be used inside benchmarks and load testing. See related notes {@link org.HdrHistogram.AbstractHistogram#recordValueWithExpectedInterval(long, long)}
     * for more explanations about coordinated omission and expected interval correction.
     * </p>
     *
     * @param expectedIntervalBetweenValueSamples If expectedIntervalBetweenValueSamples is larger than 0,
     *                                            then each time on value writing, reservoir will add auto-generated value records as appropriate if value is larger
     *                                            than expectedIntervalBetweenValueSamples
     * @return this builder instance
     * @see org.HdrHistogram.AbstractHistogram#recordValueWithExpectedInterval(long, long)
     */
    public RollingHdrHistogramBuilder withExpectedIntervalBetweenValueSamples(long expectedIntervalBetweenValueSamples) {
        recorderSettings.setExpectedIntervalBetweenValueSamples(expectedIntervalBetweenValueSamples);
        return this;
    }

    /**
     * Configures the period for which taken snapshot will be cached.
     *
     * @param duration the period for which taken snapshot will be cached, should be a positive duration.
     * @return this builder instance
     */
    public RollingHdrHistogramBuilder withSnapshotCachingDuration(Duration duration) {
        if (duration.isNegative()) {
            throw new IllegalArgumentException(duration + " is negative");
        }
        if (duration.isZero()) {
            this.snapshotCachingDuration = Optional.empty();
        } else {
            this.snapshotCachingDuration = Optional.of(duration);
        }
        return this;
    }

    /**
     * Configures list of percentiles which you plan to store in monitoring database.
     * <p>
     * This method is useful when you already know list of percentiles which need to be stored in monitoring database,
     * then you can specify it to optimize snapshot size, as result unnecessary garbage will be avoided, memory in snapshot will allocated only for percentiles which you configure.
     * </p>
     * <p> Moreover by default builder already configured with default list of percentiles {@link RecorderSettings#DEFAULT_PERCENTILES}.
     * the default percentiles are <code>double[] {0.5, 0.75, 0.9, 0.95, 0.98, 0.99, 0.999}</code>
     *
     * @param predefinedPercentiles list of percentiles which you plan to store in monitoring database, should be not empty array of doubles between {@literal 0..1}
     * @return this builder instance
     * @see #withoutSnapshotOptimization()
     */
    public RollingHdrHistogramBuilder withPredefinedPercentiles(double[] predefinedPercentiles) {
        recorderSettings.setPredefinedPercentiles(predefinedPercentiles);
        return this;
    }

    /**
     * Discards snapshot memory footprint optimization. Use this method when you do not know concrete percentiles which you need.
     * Pay attention that when you discard snapshot optimization then garbage required for take one snapshot will approximately equals to histogram size.
     * <p>
     * This method zeroes predefinedPercentiles configured by default {@link RecorderSettings#DEFAULT_PERCENTILES} or configured via {@link #withPredefinedPercentiles(double[])}.
     *
     * @return this builder instance
     */
    public RollingHdrHistogramBuilder withoutSnapshotOptimization() {
        recorderSettings.withoutSnapshotOptimization();
        return this;
    }

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
     * @return this builder instance
     */
    public RollingHdrHistogramBuilder withBackgroundExecutor(Executor backgroundExecutor) {
        recorderSettings.setBackgroundExecutor(backgroundExecutor);
        return this;
    }

    /**
     * TODO
     *
     * @param ticker
     * @return
     */
    public RollingHdrHistogramBuilder withTicker(Ticker ticker) {
        this.ticker = Objects.requireNonNull(ticker);
        return this;
    }

    /**
     * TODO
     *
     * @return
     */
    public RollingHdrHistogram build() {
        recorderSettings.validateParameters();
        RollingHdrHistogram rollingHdrHistogram = builders.get(retentionPolicy.getClass()).create(recorderSettings, retentionPolicy, ticker);
        return wrapAroundByDecorators(rollingHdrHistogram);
    }

    /**
     * Provide a (conservatively high) estimate of the Reservoir's total footprint in bytes
     *
     * @return a (conservatively high) estimate of the Reservoir's total footprint in bytes
     */
    public int getEstimatedFootprintInBytes() {
        return build().getEstimatedFootprintInBytes();
    }

    @Override
    public String toString() {
        return "RollingHdrHistogramBuilder{" +
                "retentionPolicy=" + retentionPolicy +
                ", snapshotCachingDurationMillis=" + snapshotCachingDuration +
                ", recorderSettings=" + recorderSettings +
                '}';
    }

    private final RetentionPolicy retentionPolicy;
    private RecorderSettings recorderSettings;
    private Optional<Duration> snapshotCachingDuration;
    private Ticker ticker;

    RollingHdrHistogramBuilder(RetentionPolicy retentionPolicy) {
        if (!builders.containsKey(retentionPolicy.getClass())) {
            throw new IllegalArgumentException("Unknown retention policy " + retentionPolicy.getClass());
        }
        this.retentionPolicy =  retentionPolicy;
        this.ticker = Ticker.defaultTicker();
        this.snapshotCachingDuration = Optional.empty();
        this.recorderSettings = new RecorderSettings();
    }

    private RollingHdrHistogram wrapAroundByDecorators(RollingHdrHistogram histogram) {
        // wrap around by decorator if snapshotCachingDurationMillis was specified
        if (snapshotCachingDuration.isPresent()) {
            histogram = new SnapshotCachingRollingHdrHistogram(histogram, snapshotCachingDuration.get(), ticker);
        }
        return histogram;
    }

    private static Map<Class<? extends RetentionPolicy>, HistogramFactory> builders = new HashMap<>();
    static {
        builders.put(UniformRetentionPolicy.class, (settings, retentionPolicy, ticker) -> new UniformRollingHdrHistogramImpl(settings));
        builders.put(ResetOnSnapshotRetentionPolicy.class, (settings, retentionPolicy, ticker) -> new ResetOnSnapshotRollingHdrHistogramImpl(settings));
        builders.put(ResetPeriodicallyRetentionPolicy.class, (settings, retentionPolicy, ticker) ->
                new ResetByChunksRollingHdrHistogramImpl(settings, (ResetPeriodicallyRetentionPolicy) retentionPolicy, ticker));
        builders.put(ResetPeriodicallyByChunksRetentionPolicy.class, (settings, retentionPolicy, ticker) ->
                new ResetByChunksRollingHdrHistogramImpl(settings, (ResetPeriodicallyByChunksRetentionPolicy) retentionPolicy, ticker));
    }

    private interface HistogramFactory {

        RollingHdrHistogram create(RecorderSettings settings, RetentionPolicy retentionPolicy, Ticker ticker);

    }

}
