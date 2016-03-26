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

public class HdrBuilder {

    public static int DEFAULT_NUMBER_OF_SIGNIFICANT_DIGITS = 2;
    public static AccumulationStrategy DEFAULT_ACCUMULATION_STRATEGY = ResetOnSnapshotAccumulationStrategy.INSTANCE;
    public static double[] DEFAULT_PERCENTILES = new double[] {0.5, 0.75, 0.9, 0.95, 0.98, 0.99, 0.999};

    public HdrBuilder() {
        this(WallClock.INSTANCE);
    }

    /**
     * Reservoir configured with this strategy will be cleared each time when snapshot taken.
     *
     * <p>This is default strategy for {@link HdrBuilder}
     */
    public HdrBuilder resetResevoirOnSnapshot() {
        accumulationStrategy = ResetOnSnapshotAccumulationStrategy.INSTANCE;
        return this;
    }

    /**
     * Reservoir configured with this strategy will store all measures since the reservoir was created.
     */
    public HdrBuilder neverResetResevoir() {
        accumulationStrategy = UniformAccumulationStrategy.INSTANCE;
        return this;
    }

    /**
     * Reservoir configured with this strategy will be cleared after each {@code resettingPeriod}.
     *
     * @param resettingPeriod specifies how often need to reset reservoir
     */
    public HdrBuilder resetResevoirPeriodically(Duration resettingPeriod) {
        accumulationStrategy = new ResetPeriodicallyAccumulationStrategy(resettingPeriod);
        return this;
    }

    public HdrBuilder withSignificantDigits(int numberOfSignificantValueDigits) {
        if ((numberOfSignificantValueDigits < 0) || (numberOfSignificantValueDigits > 5)) {
            throw new IllegalArgumentException("numberOfSignificantValueDigits must be between 0 and 5");
        }
        this.numberOfSignificantValueDigits = numberOfSignificantValueDigits;
        return this;
    }

    public HdrBuilder withLowestDiscernibleValue(long lowestDiscernibleValue) {
        if (lowestDiscernibleValue < 1) {
            throw new IllegalArgumentException("lowestDiscernibleValue must be >= 1");
        }
        this.lowestDiscernibleValue = Optional.of(lowestDiscernibleValue);
        return this;
    }

    public HdrBuilder withHighestTrackableValue(long highestTrackableValue, OverflowResolver overflowHandling) {
        if (highestTrackableValue < 2) {
            throw new IllegalArgumentException("highestTrackableValue must be >= 2");
        }
        this.highestTrackableValue = Optional.of(highestTrackableValue);
        this.overflowResolver = Optional.of(overflowHandling);
        return this;
    }

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

    public Timer buildAndRegisterTimer(MetricRegistry registry, String name) {
        Timer timer = buildTimer();
        registry.register(name, timer);
        return timer;
    }

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
