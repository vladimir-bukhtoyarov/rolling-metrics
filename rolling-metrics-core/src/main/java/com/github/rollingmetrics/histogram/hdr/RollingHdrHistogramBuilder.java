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
import com.github.rollingmetrics.histogram.hdr.impl.*;
import com.github.rollingmetrics.histogram.hdr.impl.ResetOnSnapshotRollingHdrHistogramImpl;
import com.github.rollingmetrics.util.ResilientExecutionUtil;
import com.github.rollingmetrics.util.Ticker;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

/**
 * The builder for creation and registration histograms, timers and reservoirs.
 *
 * <p><br> Basic examples of usage:
 * <pre><code>
 *         RollingHdrHistogramBuilder builder = RollingHdrHistogramBuilder();
 *
 *         // build and register timer
 *         Timer timer1 = builder.buildAndRegisterTimer(registry, "my-timer-1");
 *
 *         // build and register timer in another way
 *         Timer timer2 = builder.buildTimer();
 *         registry.register(timer2, "my-timer-2");
 *
 *         // build and register histogram
 *         Histogram histogram1 = builder.buildAndRegisterHistogram(registry, "my-histogram-1");
 *
 *         // build and register histogram in another way
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

    // meaningful limits to disallow user to kill performance(or memory footprint) by mistake
    static final int MAX_CHUNKS = 60;
    static final long MIN_CHUNK_RESETTING_INTERVAL_MILLIS = 1000;

    static int DEFAULT_NUMBER_OF_SIGNIFICANT_DIGITS = 2;
    static AccumulationFactory DEFAULT_ACCUMULATION_STRATEGY = AccumulationFactory.UNIFORM;
    static double[] DEFAULT_PERCENTILES = new double[]{0.5, 0.75, 0.9, 0.95, 0.98, 0.99, 0.999};
    static RecorderSettings DEFUALT_RECORDER_SETTINGS = new RecorderSettings(
            DEFAULT_NUMBER_OF_SIGNIFICANT_DIGITS,
            Optional.empty(),
            Optional.of(DEFAULT_PERCENTILES),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
    );

    RollingHdrHistogramBuilder() {
        this(Ticker.defaultTicker());
    }

    /**
     * Reservoir configured with this strategy will be cleared each time when snapshot taken.
     *
     * @return this builder instance
     * @see #resetReservoirPeriodically(Duration)
     * @see #resetReservoirPeriodicallyByChunks(Duration, int)
     * @see #neverResetReservoir()
     */
    public RollingHdrHistogramBuilder resetReservoirOnSnapshot() {
        accumulationFactory = AccumulationFactory.RESET_ON_SNAPSHOT;
        return this;
    }

    /**
     * Reservoir configured with this strategy will be cleared fully after each <tt>resettingPeriod</tt>.
     * <p>
     * The value recorded to reservoir will take affect at most <tt>resettingPeriod</tt> time.
     * </p>
     *
     * <p>
     *     If You use this strategy inside JEE environment,
     *     then it would be better to call {@code ResilientExecutionUtil.getInstance().shutdownBackgroundExecutor()}
     *     once in application shutdown listener,
     *     in order to avoid leaking reference to classloader through the thread which this library creates for histogram rotation in background.
     * </p>
     *
     * @param resettingPeriod specifies how often need to reset reservoir
     * @return this builder instance
     * @see #neverResetReservoir()
     * @see #resetReservoirOnSnapshot()
     * @see #resetReservoirPeriodicallyByChunks(Duration, int)
     */
    public RollingHdrHistogramBuilder resetReservoirPeriodically(Duration resettingPeriod) {
        int numberOfHistoryChunks = 0;
        return resetReservoirPeriodicallyByChunks(resettingPeriod.toMillis(), numberOfHistoryChunks);
    }

    /**
     * Reservoir configured with this strategy will be divided to <tt>numberChunks</tt> parts,
     * and one chunk will be cleared after each <tt>rollingTimeWindow / numberChunks</tt> elapsed.
     * This strategy is more smoothly then <tt>resetReservoirPeriodically</tt> because reservoir never zeroed at whole,
     * so user experience provided by <tt>resetReservoirPeriodicallyByChunks</tt> should look more pretty.
     * <p>
     * The value recorded to reservoir will take affect at least <tt>rollingTimeWindow</tt> and at most <tt>rollingTimeWindow *(1 + 1/numberChunks)</tt> time,
     * for example when you configure <tt>rollingTimeWindow=60 seconds and numberChunks=6</tt> then each value recorded to reservoir will be stored at <tt>60-70 seconds</tt>
     * </p>
     *
     * <p>
     *     If You use this strategy inside JEE environment,
     *     then it would be better to call {@code ResilientExecutionUtil.getInstance().shutdownBackgroundExecutor()}
     *     once in application shutdown listener,
     *     in order to avoid leaking reference to classloader through the thread which this library creates for histogram rotation in background.
     * </p>
     *
     * @param rollingTimeWindow the total rolling time window, any value recorded to reservoir will not be evicted from it at least <tt>rollingTimeWindow</tt>
     * @param numberChunks    specifies number of chunks by which reservoir will be slitted
     * @return this builder instance
     * @see #neverResetReservoir()
     * @see #resetReservoirOnSnapshot()
     * @see #resetReservoirPeriodically(Duration)
     */
    public RollingHdrHistogramBuilder resetReservoirPeriodicallyByChunks(Duration rollingTimeWindow, int numberChunks) {
        if (numberChunks < 2) {
            throw new IllegalArgumentException("numberHistoryChunks should be >= 2");
        }
        if (numberChunks > MAX_CHUNKS) {
            throw new IllegalArgumentException("numberHistoryChunks should be <= " + MAX_CHUNKS);
        }
        long resettingPeriodMillis = rollingTimeWindow.toMillis() / numberChunks;
        return resetReservoirPeriodicallyByChunks(resettingPeriodMillis, numberChunks);
    }

    /**
     * Reservoir configured with this strategy will store all values since the reservoir was created.
     *
     * <p>This is default strategy for {@link RollingHdrHistogramBuilder}
     *
     * @return this builder instance
     * @see #resetReservoirPeriodically(Duration)
     * @see #resetReservoirOnSnapshot()
     * @see #resetReservoirPeriodicallyByChunks(Duration, int)
     */
    public RollingHdrHistogramBuilder neverResetReservoir() {
        accumulationFactory = AccumulationFactory.UNIFORM;
        return this;
    }

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
        this.recorderSettings = recorderSettings.withSignificantDigits(numberOfSignificantValueDigits);
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
        this.recorderSettings = recorderSettings.withLowestDiscernibleValue(lowestDiscernibleValue);
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
        this.recorderSettings = recorderSettings.withHighestTrackableValue(highestTrackableValue, overflowResolver);
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
        this.recorderSettings = recorderSettings.withExpectedIntervalBetweenValueSamples(expectedIntervalBetweenValueSamples);
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
     * <p> Moreover by default builder already configured with default list of percentiles {@link #DEFAULT_PERCENTILES}.
     * the default percentiles are <code>double[] {0.5, 0.75, 0.9, 0.95, 0.98, 0.99, 0.999}</code>
     *
     * @param predefinedPercentiles list of percentiles which you plan to store in monitoring database, should be not empty array of doubles between {@literal 0..1}
     * @return this builder instance
     * @see #withoutSnapshotOptimization()
     */
    public RollingHdrHistogramBuilder withPredefinedPercentiles(double[] predefinedPercentiles) {
        this.recorderSettings = recorderSettings.withPredefinedPercentiles(predefinedPercentiles);
        return this;
    }

    /**
     * Discards snapshot memory footprint optimization. Use this method when you do not know concrete percentiles which you need.
     * Pay attention that when you discard snapshot optimization then garbage required for take one snapshot will approximately equals to histogram size.
     * <p>
     * This method zeroes predefinedPercentiles configured by default {@link #DEFAULT_PERCENTILES} or configured via {@link #withPredefinedPercentiles(double[])}.
     *
     * @return this builder instance
     */
    public RollingHdrHistogramBuilder withoutSnapshotOptimization() {
        this.recorderSettings = recorderSettings.withoutSnapshotOptimization();
        return this;
    }

    /**
     * Configures the executor which will be used if any of {@link #resetReservoirPeriodically(Duration)} or {@link #resetReservoirPeriodicallyByChunks(Duration, int)} (Duration)}.
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
        if (backgroundExecutor == null) {
            throw new IllegalArgumentException("backgroundExecutor must not be null");
        }
        this.backgroundExecutor = Optional.of(backgroundExecutor);
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
        RollingHdrHistogram rollingHdrHistogram = accumulationFactory.createAccumulator(recorderSettings, ticker);
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

    /**
     * Creates full copy of this builder.
     *
     * @return copy of this builder
     */
    public RollingHdrHistogramBuilder deepCopy() {
        return new RollingHdrHistogramBuilder(ticker, accumulationFactory, snapshotCachingDuration, recorderSettings, backgroundExecutor);
    }

    @Override
    public String toString() {
        return "RollingHdrHistogramBuilder{" +
                "accumulationStrategy=" + accumulationFactory +
                ", snapshotCachingDurationMillis=" + snapshotCachingDuration +
                ", recorderSettings=" + recorderSettings +
                '}';
    }

    private AccumulationFactory accumulationFactory;

    private RecorderSettings recorderSettings;

    private Optional<Executor> backgroundExecutor;
    private Optional<Duration> snapshotCachingDuration;

    private Ticker ticker;

    public RollingHdrHistogramBuilder(Ticker ticker) {
        this(ticker, DEFAULT_ACCUMULATION_STRATEGY, Optional.empty(), DEFUALT_RECORDER_SETTINGS, Optional.empty());
    }

    private RollingHdrHistogramBuilder(Ticker ticker,
                                       AccumulationFactory accumulationFactory,
                                       Optional<Duration> snapshotCachingDuration,
                                       RecorderSettings recorderSettings,
                                       Optional<Executor> backgroundExecutor) {
        this.ticker = ticker;
        this.accumulationFactory = accumulationFactory;
        this.snapshotCachingDuration = snapshotCachingDuration;
        this.recorderSettings = recorderSettings;
        this.backgroundExecutor = backgroundExecutor;
    }

    private RollingHdrHistogramBuilder resetReservoirPeriodicallyByChunks(long resettingPeriodMillis, int numberHistoryChunks) {
        if (resettingPeriodMillis <= 0) {
            throw new IllegalArgumentException("resettingPeriod must be a positive duration");
        }
        if (resettingPeriodMillis < MIN_CHUNK_RESETTING_INTERVAL_MILLIS) {
            throw new IllegalArgumentException("Interval between resetting must be >= " + MIN_CHUNK_RESETTING_INTERVAL_MILLIS + " millis");
        }

        accumulationFactory = (recorder, ticker) -> new ResetByChunksRollingHdrHistogramImpl(recorder, numberHistoryChunks, resettingPeriodMillis, ticker, getExecutor());
        return this;
    }

    private Executor getExecutor() {
        return backgroundExecutor.orElseGet(ResilientExecutionUtil.getInstance()::getBackgroundExecutor);
    }

    private RollingHdrHistogram wrapAroundByDecorators(RollingHdrHistogram histogram) {
        // wrap around by decorator if snapshotCachingDurationMillis was specified
        if (snapshotCachingDuration.isPresent()) {
            histogram = new SnapshotCachingRollingHdrHistogram(histogram, snapshotCachingDuration.get(), ticker);
        }
        return histogram;
    }

    interface AccumulationFactory {

        AccumulationFactory UNIFORM = (settings, ticker) -> new UniformRollingHdrHistogramImpl(settings);

        AccumulationFactory RESET_ON_SNAPSHOT = (settings, ticker) -> new ResetOnSnapshotRollingHdrHistogramImpl(settings);

        RollingHdrHistogram createAccumulator(RecorderSettings settings, Ticker ticker);

    }

}
