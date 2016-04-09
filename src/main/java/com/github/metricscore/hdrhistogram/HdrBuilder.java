/*
 *
 *  Copyright 2016 Vladimir Bukhtoyarov
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

package com.github.metricscore.hdrhistogram;

import com.codahale.metrics.*;
import com.github.metricscore.hdrhistogram.accumulator.Accumulator;
import com.github.metricscore.hdrhistogram.accumulator.ResetByChunksAccumulator;
import com.github.metricscore.hdrhistogram.accumulator.ResetOnSnapshotAccumulator;
import com.github.metricscore.hdrhistogram.accumulator.UniformAccumulator;
import org.HdrHistogram.Recorder;

import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * The entry point of metrics-core-hdr library which can be used for creation and registration histograms, timers and reservoirs.
 *
 * <p><br> Basic examples of usage:
 * <pre><code>
 *         HdrBuilder builder = HdrBuilder();
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
 *         HdrBuilder builder = new HdrBuilder().withSignificantDigits(3);
 *         System.out.println(builder.getEstimatedFootprintInBytes());
 * </code>
 * </pre>
 *
 * @see org.HdrHistogram.Histogram
 */
public class HdrBuilder {

    // meaningful limits to disallow user to kill performance(or memory footprint) by mistake
    static final int MAX_CHUNKS = 25;
    static final long MIN_CHUNK_RESETTING_INTERVAL_MILLIS = 1000;

    static int DEFAULT_NUMBER_OF_SIGNIFICANT_DIGITS = 2;
    static AccumulationFactory DEFAULT_ACCUMULATION_STRATEGY = AccumulationFactory.UNIFORM;
    static double[] DEFAULT_PERCENTILES = new double[]{0.5, 0.75, 0.9, 0.95, 0.98, 0.99, 0.999};

    public HdrBuilder() {
        this(Clock.defaultClock());
    }

    /**
     * Reservoir configured with this strategy will be cleared each time when snapshot taken.
     *
     * @return this builder instance
     * @see #resetReservoirPeriodically(Duration)
     * @see #neverResetReservoir()
     */
    public HdrBuilder resetReservoirOnSnapshot() {
        accumulationFactory = AccumulationFactory.RESET_ON_SNAPSHOT;
        return this;
    }

    /**
     * Reservoir configured with this strategy will be cleared fully after each <tt>resettingPeriod</tt>.
     * <p>
     * The measure written to reservoir will take affect at most <tt>resettingPeriod</tt> time.
     * </p>
     * <p>
     * This is equivalent for {@code resetReservoirByChunks(resettingPeriod, 1)}
     * </p>
     *
     * @param resettingPeriod specifies how often need to reset reservoir
     * @return this builder instance
     * @see #neverResetReservoir()
     * @see #resetReservoirOnSnapshot()
     * @see #resetReservoirByChunks(Duration, int)
     */
    public HdrBuilder resetReservoirPeriodically(Duration resettingPeriod) {
        return resetReservoirByChunks(resettingPeriod, 1);
    }

    /**
     * Reservoir configured with this strategy will be divided to <tt>numberChunks</tt> parts,
     * and one chunk will be cleared after each <tt>resettingPeriod</tt>.
     * This strategy is more smoothly then <tt>resetReservoirPeriodically</tt> because reservoir never zeroyed at whole,
     * so user experience provided by <tt>resetReservoirByChunks</tt> should look more pretty.
     * <p>
     * The measure written to reservoir will take affect at least <tt>resettingPeriod * (numberChunks - 1)</tt> and at most <tt>resettingPeriod * numberChunks</tt> time,
     * for example when you configure <tt>resettingPeriod=10 seconds and numberChunks=6</tt> then each measure written to reservoir will be stored at <tt>50-60 seconds</tt>
     * </p>
     *
     * @param resettingPeriod specifies interval between chunk resetting
     * @param numberChunks    specifies number of chunks by which reservoir will be slitted
     * @return this builder instance
     * @see #neverResetReservoir()
     * @see #resetReservoirOnSnapshot()
     * @see #resetReservoirPeriodically(Duration)
     */
    public HdrBuilder resetReservoirByChunks(Duration resettingPeriod, int numberChunks) {
        if (resettingPeriod.isNegative() || resettingPeriod.isZero()) {
            throw new IllegalArgumentException("resettingPeriod must be a positive duration");
        }
        if (resettingPeriod.toMillis() < MIN_CHUNK_RESETTING_INTERVAL_MILLIS) {
            throw new IllegalArgumentException("resettingPeriod must be >= " + MIN_CHUNK_RESETTING_INTERVAL_MILLIS + " millis");
        }
        if (numberChunks < 1) {
            throw new IllegalArgumentException("numberChunks should be >= 1");
        }
        if (numberChunks > MAX_CHUNKS) {
            throw new IllegalArgumentException("numberChunks should be <= " + MAX_CHUNKS);
        }
        accumulationFactory = (recorder, clock) -> new ResetByChunksAccumulator(recorder, numberChunks, resettingPeriod.toMillis(), clock);
        return this;
    }

    /**
     * Reservoir configured with this strategy will store all measures since the reservoir was created.
     *
     * <p>This is default strategy for {@link HdrBuilder}
     *
     * @return this builder instance
     * @see #resetReservoirPeriodically(Duration)
     * @see #resetReservoirOnSnapshot()
     * @see #resetReservoirByChunks(Duration, int)
     */
    public HdrBuilder neverResetReservoir() {
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
    public HdrBuilder withSignificantDigits(int numberOfSignificantValueDigits) {
        if ((numberOfSignificantValueDigits < 0) || (numberOfSignificantValueDigits > 5)) {
            throw new IllegalArgumentException("numberOfSignificantValueDigits must be between 0 and 5");
        }
        this.numberOfSignificantValueDigits = numberOfSignificantValueDigits;
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
    public HdrBuilder withLowestDiscernibleValue(long lowestDiscernibleValue) {
        if (lowestDiscernibleValue < 1) {
            throw new IllegalArgumentException("lowestDiscernibleValue must be >= 1");
        }
        this.lowestDiscernibleValue = Optional.of(lowestDiscernibleValue);
        return this;
    }

    /**
     * Configures the highest value to be tracked by the histogram.
     *
     * @param highestTrackableValue highest value to be tracked by the histogram. Must be a positive integer that is {@literal >=} (2 * lowestDiscernibleValue)
     * @param overflowResolver      specifies behavior which should be applied when writing to reservoir value which greater than highestTrackableValue
     * @return this builder instance
     */
    public HdrBuilder withHighestTrackableValue(long highestTrackableValue, OverflowResolver overflowResolver) {
        if (highestTrackableValue < 2) {
            throw new IllegalArgumentException("highestTrackableValue must be >= 2");
        }
        this.highestTrackableValue = Optional.of(highestTrackableValue);
        this.overflowResolver = Optional.of(overflowResolver);
        return this;
    }

    /**
     * When this setting is configured then it will be used to compensate for the loss of sampled values when a recorded value is larger than the expected interval between value samples,
     * Histogram will auto-generate an additional series of decreasingly-smaller (down to the expectedIntervalBetweenValueSamples) value records.
     *
     * <p>
     * <font color="red">  WARNING:</font> You should not use this method for monitoring your application in the production,
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
    public HdrBuilder withExpectedIntervalBetweenValueSamples(long expectedIntervalBetweenValueSamples) {
        this.expectedIntervalBetweenValueSamples = Optional.of(expectedIntervalBetweenValueSamples);
        return this;
    }

    /**
     * Configures the period for which taken snapshot will be cached.
     *
     * @param duration the period for which taken snapshot will be cached, should be a positive duration.
     * @return this builder instance
     */
    public HdrBuilder withSnapshotCachingDuration(Duration duration) {
        if (duration.isNegative()) {
            throw new IllegalArgumentException(duration + " is negative");
        }
        if (duration.isZero()) {
            this.snapshotCachingDurationMillis = Optional.empty();
        } else {
            this.snapshotCachingDurationMillis = Optional.of(duration.toMillis());
        }
        return this;
    }

    /**
     * Configures list of percentiles which you plan to store in monitoring database.
     * <p>
     * This method is useful when you already know list of percentiles which need to be stored in monitoring database,
     * then you can specify it to optimize snapshot size, as result unnecessary garbage will be avoided, memory in snapshot will allocated only for percentiles which you configure.
     * </p>
     * <p> Moreover by default builder already configured with default list of percentiles {@link #DEFAULT_PERCENTILES} which tightly compatible with {@link com.codahale.metrics.JmxReporter},
     * the default percentiles are <code>double[] {0.5, 0.75, 0.9, 0.95, 0.98, 0.99, 0.999}</code>
     *
     * @param predefinedPercentiles list of percentiles which you plan to store in monitoring database, should be not empty array of doubles between {@literal 0..1}
     * @return this builder instance
     * @see #withoutSnapshotOptimization()
     */
    public HdrBuilder withPredefinedPercentiles(double[] predefinedPercentiles) {
        predefinedPercentiles = Objects.requireNonNull(predefinedPercentiles, "predefinedPercentiles array should not be null");
        if (predefinedPercentiles.length == 0) {
            String msg = "predefinedPercentiles.length is zero. Use withoutSnapshotOptimization() instead of passing empty array.";
            throw new IllegalArgumentException(msg);
        }

        for (double percentile : predefinedPercentiles) {
            if (percentile < 0.0 || percentile > 1.0) {
                String msg = "Illegal percentiles " + Arrays.toString(predefinedPercentiles) + " - all values must be between 0 and 1";
                throw new IllegalArgumentException(msg);
            }
        }
        double[] sortedPercentiles = copyAndSort(predefinedPercentiles);
        this.predefinedPercentiles = Optional.of(sortedPercentiles);
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
    public HdrBuilder withoutSnapshotOptimization() {
        this.predefinedPercentiles = Optional.empty();
        return this;
    }

    /**
     * Builds reservoir which can be useful for building monitoring primitives with higher level of abstraction.
     *
     * @return an instance of {@link com.codahale.metrics.Reservoir}
     */
    public Reservoir buildReservoir() {
        Reservoir reservoir = buildHdrReservoir();
        reservoir = wrapAroundByDecorators(reservoir);
        return reservoir;
    }

    /**
     * Builds histogram.
     *
     * @return an instance of {@link com.codahale.metrics.Histogram}
     * @see #buildAndRegisterHistogram(MetricRegistry, String)
     */
    public Histogram buildHistogram() {
        return new Histogram(buildReservoir());
    }

    /**
     * Builds and registers histogram.
     *
     * @param registry metric registry in which constructed histogram will be registered
     * @param name     the name under with constructed histogram will be registered in the {@code registry}
     * @return an instance of {@link com.codahale.metrics.Histogram}
     * @see #buildHistogram()
     */
    public Histogram buildAndRegisterHistogram(MetricRegistry registry, String name) {
        Histogram histogram = buildHistogram();
        registry.register(name, histogram);
        return histogram;
    }

    /**
     * Builds timer.
     *
     * @return an instance of {@link com.codahale.metrics.Timer}
     * @see #buildAndRegisterTimer(MetricRegistry, String)
     */
    public Timer buildTimer() {
        return new Timer(buildReservoir());
    }

    /**
     * Builds and registers timer.
     *
     * @param registry metric registry in which constructed histogram will be registered
     * @param name     the name under with constructed timer will be registered in the {@code registry}
     * @return an instance of {@link com.codahale.metrics.Timer}
     * @see #buildTimer()
     */
    public Timer buildAndRegisterTimer(MetricRegistry registry, String name) {
        Timer timer = buildTimer();
        registry.register(name, timer);
        return timer;
    }

    /**
     * Provide a (conservatively high) estimate of the Reservoir's total footprint in bytes
     *
     * @return a (conservatively high) estimate of the Reservoir's total footprint in bytes
     */
    public int getEstimatedFootprintInBytes() {
        HdrReservoir hdrReservoir = buildHdrReservoir();
        return hdrReservoir.getEstimatedFootprintInBytes();
    }

    /**
     * Creates full copy of this builder.
     *
     * @return copy of this builder
     */
    public HdrBuilder deepCopy() {
        return new HdrBuilder(clock, accumulationFactory, numberOfSignificantValueDigits, predefinedPercentiles, lowestDiscernibleValue,
                highestTrackableValue, overflowResolver, snapshotCachingDurationMillis, expectedIntervalBetweenValueSamples);
    }

    @Override
    public String toString() {
        return "HdrBuilder{" +
                "accumulationStrategy=" + accumulationFactory +
                ", numberOfSignificantValueDigits=" + numberOfSignificantValueDigits +
                ", lowestDiscernibleValue=" + lowestDiscernibleValue +
                ", highestTrackableValue=" + highestTrackableValue +
                ", overflowResolver=" + overflowResolver +
                ", snapshotCachingDurationMillis=" + snapshotCachingDurationMillis +
                ", predefinedPercentiles=" + Arrays.toString(predefinedPercentiles.orElse(new double[0])) +
                '}';
    }

    private AccumulationFactory accumulationFactory;
    private int numberOfSignificantValueDigits;
    private Optional<Long> lowestDiscernibleValue;
    private Optional<Long> highestTrackableValue;
    private Optional<OverflowResolver> overflowResolver;
    private Optional<Long> snapshotCachingDurationMillis;
    private Optional<double[]> predefinedPercentiles;
    private Optional<Long> expectedIntervalBetweenValueSamples;

    private Clock clock;

    public HdrBuilder(Clock clock) {
        this(clock, DEFAULT_ACCUMULATION_STRATEGY, DEFAULT_NUMBER_OF_SIGNIFICANT_DIGITS, Optional.of(DEFAULT_PERCENTILES), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    private HdrBuilder(Clock clock,
                       AccumulationFactory accumulationFactory,
                       int numberOfSignificantValueDigits,
                       Optional<double[]> predefinedPercentiles,
                       Optional<Long> lowestDiscernibleValue,
                       Optional<Long> highestTrackableValue,
                       Optional<OverflowResolver> overflowResolver,
                       Optional<Long> snapshotCachingDurationMillis,
                       Optional<Long> expectedIntervalBetweenValueSamples) {
        this.clock = clock;
        this.accumulationFactory = accumulationFactory;
        this.numberOfSignificantValueDigits = numberOfSignificantValueDigits;
        this.lowestDiscernibleValue = lowestDiscernibleValue;
        this.highestTrackableValue = highestTrackableValue;
        this.overflowResolver = overflowResolver;
        this.snapshotCachingDurationMillis = snapshotCachingDurationMillis;
        this.predefinedPercentiles = predefinedPercentiles;
        this.expectedIntervalBetweenValueSamples = expectedIntervalBetweenValueSamples;
    }

    private HdrReservoir buildHdrReservoir() {
        validateParameters();
        Accumulator accumulator = accumulationFactory.createAccumulator(this::buildRecorder, clock);
        return new HdrReservoir(accumulator, predefinedPercentiles, highestTrackableValue, overflowResolver, expectedIntervalBetweenValueSamples);
    }

    private void validateParameters() {
        if (highestTrackableValue.isPresent() && lowestDiscernibleValue.isPresent() && highestTrackableValue.get() < 2L * lowestDiscernibleValue.get()) {
            throw new IllegalStateException("highestTrackableValue must be >= 2 * lowestDiscernibleValue");
        }

        if (lowestDiscernibleValue.isPresent() && !highestTrackableValue.isPresent()) {
            throw new IllegalStateException("lowestDiscernibleValue is specified but highestTrackableValue undefined");
        }
    }

    private Recorder buildRecorder() {
        if (lowestDiscernibleValue.isPresent()) {
            return new Recorder(lowestDiscernibleValue.get(), highestTrackableValue.get(), numberOfSignificantValueDigits);
        }
        if (highestTrackableValue.isPresent()) {
            return new Recorder(highestTrackableValue.get(), numberOfSignificantValueDigits);
        }
        return new Recorder(numberOfSignificantValueDigits);
    }

    private Reservoir wrapAroundByDecorators(Reservoir reservoir) {
        // wrap around by decorator if snapshotCachingDurationMillis was specified
        if (snapshotCachingDurationMillis.isPresent()) {
            reservoir = new SnapshotCachingReservoir(reservoir, snapshotCachingDurationMillis.get(), clock);
        }
        return reservoir;
    }

    private static double[] copyAndSort(double[] predefinedPercentiles) {
        double[] sortedPercentiles = Arrays.copyOf(predefinedPercentiles, predefinedPercentiles.length);
        Arrays.sort(sortedPercentiles);
        return sortedPercentiles;
    }

    interface AccumulationFactory {

        AccumulationFactory UNIFORM = (recorderSupplier, clock) -> new UniformAccumulator(recorderSupplier.get());

        AccumulationFactory RESET_ON_SNAPSHOT = (recorderSupplier, clock) -> new ResetOnSnapshotAccumulator(recorderSupplier.get());

        Accumulator createAccumulator(Supplier<Recorder> recorderSupplier, Clock clock);

    }

}
