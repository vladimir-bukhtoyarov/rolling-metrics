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

import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Timer;
import org.HdrHistogram.Recorder;

import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * The entry point of metrics-core-hdr library which can be used for creation and registration histograms, timers and reservoirs.
 *
 * This builder provides ability to configure:
 * <ul>
 * <li><b>numberOfSignificantValueDigits</b> The number of significant decimal digits to which the histogram will maintain value resolution and separation, see {@link #withSignificantDigits(int)}.</li>
 * <li><b>lowestDiscernibleValue</b> The lowest value that can be discerned (distinguished from 0) by the histogram, see {@link #withLowestDiscernibleValue(long)}</li>
 * <li><b>highestTrackableValue</b> The highest value to be tracked by the histogram, see {@link #withHighestTrackableValue(long, OverflowResolver)}</li>
 * <li><b>predefinedPercentiles</b> If you already know list of percentiles which need to be stored in monitoring database,
 * then you can specify it to optimize snapshot size, as result unnecessary garbage will be avoided, see {@link #withPredefinedPercentiles(double[])}</li>
 * <li><b>Different strategies of reservoir resetting</b></li>HdrHistogram loses nothing it is good and bad in same time.
 * It is good because you do not lose min and max,
 * and it is bad because in real world use-cases you need to show measurements which actual to current moment of time.
 * So  you need the way to kick out obsolete values for reservoir. Metrics-Core-Hdr provides out of the box three different solutions: {@link #resetResevoirOnSnapshot()},{@link #resetResevoirPeriodically(Duration)}, {@link #neverResetResevoir()}.
 * <li><b>Snapshot caching duration</b> If you use any legacy monitoring system like Zabbix which pull measures from application instead of let application to store measures in monitoring database,
 * then you need to think about snapshot atomicity. For example if you collect 95 percentile, 99 percentile and mean then it would be better to store in database all three measures from same snapshot
 * otherwise you can show something unbelievable on the monitoring screens when in same time 95 percentile is greater then 99 percentile.
 * There is no solution which guaranties 100% atomicity, but caching of snapshot will be enough for many cases.
 * According to Zabbix Java Proxy solution will work in following way: imagine that Zabbix collects measures from your application each 60seconds and you configured reservoir to snapshotCachingDuration 5 seconds,
 * in this case solution will work in following way:
 * <ol>
 *     <li>Zabbix send command to Zabbix Java Proxy with batch of metrics<li/>
 *     <li>Zabbix Java Proxy opens JMX/RMI connection to the application. And take first metric, for example 96 percentile. Snapshot is taken and cached at this moment<li/>
 *     <li>When Zabbix Java Proxy asks application for 99 percentile and mean application will answer by data cached in the snapshot, because 5 seconds did not elapsed since snapshot was created<li/>
 *     <li>Zabbix Java Proxy closes JMX/RMI connection to the application.<li/>
 *     <li>On the next iteration after one minute application will invalidate snapshot and take new because of snapshot TTL is elapsed.<li/>
 * </ol>
 * <br>see {@link #withSnapshotCachingDuration(Duration)}
 * </li>
 * </ul>
 *
 * <p/>
 * <p>An example of usage:
 * <pre>
 *     <code>
 *
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
 * <p/>
 *
 * @see com.codahale.metrics.Histogram
 */
public class HdrBuilder {

    public static int DEFAULT_NUMBER_OF_SIGNIFICANT_DIGITS = 2;
    public static AccumulationStrategy DEFAULT_ACCUMULATION_STRATEGY = ResetOnSnapshotAccumulationStrategy.INSTANCE;
    public static double[] DEFAULT_PERCENTILES = new double[] {0.5, 0.75, 0.9, 0.95, 0.98, 0.99, 0.999};

    public HdrBuilder() {
        this(WallClock.INSTANCE);
    }

    /**
     * Reservoir configured with this strategy will be cleared each time when snapshot taken.
     * <p/>
     * <p>This is default strategy for {@link HdrBuilder}
     *
     * @see #resetResevoirPeriodically(Duration)
     * @see #neverResetResevoir()
     *
     * @return this builder instance
     */
    public HdrBuilder resetResevoirOnSnapshot() {
        accumulationStrategy = ResetOnSnapshotAccumulationStrategy.INSTANCE;
        return this;
    }

    /**
     * Reservoir configured with this strategy will store all measures since the reservoir was created.
     *
     * @see #resetResevoirPeriodically(Duration)
     * @see #resetResevoirOnSnapshot()
     * @see UniformAccumulationStrategy
     *
     * @return this builder instance
     */
    public HdrBuilder neverResetResevoir() {
        accumulationStrategy = UniformAccumulationStrategy.INSTANCE;
        return this;
    }

    /**
     * Reservoir configured with this strategy will be cleared after each {@code resettingPeriod}.
     *
     * @param resettingPeriod specifies how often need to reset reservoir
     * @see #neverResetResevoir()
     * @see #resetResevoirOnSnapshot()
     *
     * @return this builder instance
     */
    public HdrBuilder resetResevoirPeriodically(Duration resettingPeriod) {
        accumulationStrategy = new ResetPeriodicallyAccumulationStrategy(resettingPeriod);
        return this;
    }

    /**
     * @param numberOfSignificantValueDigits The number of significant decimal digits to which the histogram will
     *                                       maintain value resolution and separation. Must be a non-negative
     *                                       integer between 0 and 5.
     *
     * @return this builder instance
     */
    public HdrBuilder withSignificantDigits(int numberOfSignificantValueDigits) {
        if ((numberOfSignificantValueDigits < 0) || (numberOfSignificantValueDigits > 5)) {
            throw new IllegalArgumentException("numberOfSignificantValueDigits must be between 0 and 5");
        }
        this.numberOfSignificantValueDigits = numberOfSignificantValueDigits;
        return this;
    }

    /**
     * @param lowestDiscernibleValue
     *
     * @return @return this builder instance
     */
    public HdrBuilder withLowestDiscernibleValue(long lowestDiscernibleValue) {
        if (lowestDiscernibleValue < 1) {
            throw new IllegalArgumentException("lowestDiscernibleValue must be >= 1");
        }
        this.lowestDiscernibleValue = Optional.of(lowestDiscernibleValue);
        return this;
    }

    /**
     *
     * @param highestTrackableValue
     * @param overflowHandling
     *
     * @return @return this builder instance
     */
    public HdrBuilder withHighestTrackableValue(long highestTrackableValue, OverflowResolver overflowHandling) {
        if (highestTrackableValue < 2) {
            throw new IllegalArgumentException("highestTrackableValue must be >= 2");
        }
        this.highestTrackableValue = Optional.of(highestTrackableValue);
        this.overflowResolver = Optional.of(overflowHandling);
        return this;
    }

    /**
     *
     * @param duration
     *
     * @return @return this builder instance
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
     *
     * @param predefinedPercentiles
     *
     * @return this builder instance
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
     *
     * @return this builder instance
     */
    public HdrBuilder withoutSnapshotOptimization() {
        this.predefinedPercentiles = Optional.empty();
        return this;
    }

    public Reservoir buildReservoir() {
        validateParameters();
        Recorder recorder = buildRecorder();
        Accumulator accumulator = accumulationStrategy.createAccumulator(recorder, wallClock);
        Reservoir reservoir = new HdrReservoir(accumulator, predefinedPercentiles);
        reservoir = wrapAroundByDecorators(reservoir);
        return reservoir;
    }

    public Histogram buildHistogram() {
        return new Histogram(buildReservoir());
    }

    public Histogram buildAndRegisterHistogram(MetricRegistry registry, String name) {
        Histogram histogram = buildHistogram();
        registry.register(name, histogram);
        return histogram;
    }

    public Timer buildTimer() {
        return new Timer(buildReservoir());
    }

    /**
     * Builds and registers timer in registry.
     *
     * @param registry
     * @param name
     *
     * @return created and registered timer
     */
    public Timer buildAndRegisterTimer(MetricRegistry registry, String name) {
        Timer timer = buildTimer();
        registry.register(name, timer);
        return timer;
    }

    /**
     * Creates full copy of this builder.
     *
     * @return copy of this builder
     */
    public HdrBuilder clone() {
        return new HdrBuilder(wallClock, accumulationStrategy, numberOfSignificantValueDigits, predefinedPercentiles, lowestDiscernibleValue,
                highestTrackableValue, overflowResolver, snapshotCachingDurationMillis);
    }

    @Override
    public String toString() {
        return "HdrBuilder{" +
                "accumulationStrategy=" + accumulationStrategy +
                ", numberOfSignificantValueDigits=" + numberOfSignificantValueDigits +
                ", lowestDiscernibleValue=" + lowestDiscernibleValue +
                ", highestTrackableValue=" + highestTrackableValue +
                ", overflowResolver=" + overflowResolver +
                ", snapshotCachingDurationMillis=" + snapshotCachingDurationMillis +
                ", predefinedPercentiles=" + Arrays.toString(predefinedPercentiles.orElse(new double[0])) +
                '}';
    }

    private AccumulationStrategy accumulationStrategy;
    private int numberOfSignificantValueDigits;
    private Optional<Long> lowestDiscernibleValue;
    private Optional<Long> highestTrackableValue;
    private Optional<OverflowResolver> overflowResolver;
    private Optional<Long> snapshotCachingDurationMillis;
    private Optional<double[]> predefinedPercentiles;

    private WallClock wallClock;

    HdrBuilder(WallClock wallClock) {
        this(wallClock, DEFAULT_ACCUMULATION_STRATEGY, DEFAULT_NUMBER_OF_SIGNIFICANT_DIGITS, Optional.of(DEFAULT_PERCENTILES), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    private HdrBuilder(WallClock wallClock,
                       AccumulationStrategy accumulationStrategy,
                       int numberOfSignificantValueDigits,
                       Optional<double[]> predefinedPercentiles,
                       Optional<Long> lowestDiscernibleValue,
                       Optional<Long> highestTrackableValue,
                       Optional<OverflowResolver> overflowResolver,
                       Optional<Long> snapshotCachingDurationMillis) {
        this.wallClock = wallClock;
        this.accumulationStrategy = accumulationStrategy;
        this.numberOfSignificantValueDigits = numberOfSignificantValueDigits;
        this.lowestDiscernibleValue = lowestDiscernibleValue;
        this.highestTrackableValue = highestTrackableValue;
        this.overflowResolver = overflowResolver;
        this.snapshotCachingDurationMillis = snapshotCachingDurationMillis;
        this.predefinedPercentiles = predefinedPercentiles;
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
        // wrap around by decorator if highestTrackableValue was specified
        if (highestTrackableValue.isPresent()) {
            reservoir = new HighestTrackableValueAwareReservoir(reservoir, highestTrackableValue.get(), overflowResolver.get());
        }

        // wrap around by decorator if snapshotCachingDurationMillis was specified
        if (snapshotCachingDurationMillis.isPresent()) {
            reservoir = new SnapshotCachingReservoir(reservoir, snapshotCachingDurationMillis.get(), wallClock);
        }
        return reservoir;
    }

    private static double[] copyAndSort(double[] predefinedPercentiles) {
        double[] sortedPercentiles = Arrays.copyOf(predefinedPercentiles, predefinedPercentiles.length);
        Arrays.sort(sortedPercentiles);
        return sortedPercentiles;
    }

}
