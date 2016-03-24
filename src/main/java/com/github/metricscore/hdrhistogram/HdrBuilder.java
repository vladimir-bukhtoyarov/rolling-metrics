package com.github.metricscore.hdrhistogram;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Timer;

import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public class HdrBuilder {

    public static int DEFAULT_NUMBER_OF_SIGNIFICANT_DIGITS = 2;
    public static AccumulationStrategy DEFAULT_ACCUMULATION_STRATEGY = AccumulationStrategy.resetOnSnapshot();
    public static double[] DEFAULT_PERCENTILES = new double[] {0.5, 0.75, 0.95, 0.98, 0.99, 0.999};

    private AccumulationStrategy accumulationStrategy;
    private int numberOfSignificantValueDigits;
    private Optional<Long> lowestDiscernibleValue;
    private Optional<Long> highestTrackableValue;
    private Optional<OverflowHandlingStrategy> overflowHandling;
    private Optional<Long> snapshotCachingDurationMillis;
    private Optional<double[]> predefinedPercentiles;

    public HdrBuilder() {
        this(DEFAULT_ACCUMULATION_STRATEGY, DEFAULT_NUMBER_OF_SIGNIFICANT_DIGITS, Optional.of(DEFAULT_PERCENTILES), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public HdrBuilder withAccumulationStrategy(AccumulationStrategy accumulationStrategy) {
        this.accumulationStrategy = Objects.requireNonNull(accumulationStrategy, "accumulationStrategy should not be null");
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

    public HdrBuilder withHighestTrackableValue(long highestTrackableValue, OverflowHandlingStrategy overflowHandling) {
        if (highestTrackableValue < 2) {
            throw new IllegalArgumentException("highestTrackableValue must be >= 2");
        }
        this.highestTrackableValue = Optional.of(highestTrackableValue);
        this.overflowHandling = Optional.of(overflowHandling);
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

    private double[] copyAndSort(double[] predefinedPercentiles) {
        double[] sortedPercentiles = Arrays.copyOf(predefinedPercentiles, predefinedPercentiles.length);
        Arrays.sort(sortedPercentiles);
        return sortedPercentiles;
    }

    public Reservoir buildReservoir() {
        if (highestTrackableValue.isPresent() && lowestDiscernibleValue.isPresent() && highestTrackableValue.get() < 2L * lowestDiscernibleValue.get()) {
            throw new IllegalStateException("highestTrackableValue must be >= 2 * lowestDiscernibleValue");
        }
        if (lowestDiscernibleValue.isPresent() && !highestTrackableValue.isPresent()) {
            throw new IllegalStateException("lowestDiscernibleValue is specified but highestTrackableValue undefined");
        }
        HdrReservoir reservoir = new HdrReservoir(accumulationStrategy, numberOfSignificantValueDigits, lowestDiscernibleValue, highestTrackableValue, overflowHandling, predefinedPercentiles);
        if (!snapshotCachingDurationMillis.isPresent()) {
            return reservoir;
        }
        return new SnapshotCachingReservoir(reservoir, snapshotCachingDurationMillis.get(), WallClock.INSTANCE);
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
        return new HdrBuilder(accumulationStrategy, numberOfSignificantValueDigits, predefinedPercentiles, lowestDiscernibleValue,
                highestTrackableValue, overflowHandling, snapshotCachingDurationMillis);
    }

    private HdrBuilder(AccumulationStrategy accumulationStrategy,
                       int numberOfSignificantValueDigits,
                       Optional<double[]> predefinedPercentiles,
                       Optional<Long> lowestDiscernibleValue,
                       Optional<Long> highestTrackableValue,
                       Optional<OverflowHandlingStrategy> overflowHandling,
                       Optional<Long> snapshotCachingDurationMillis) {
        this.accumulationStrategy = accumulationStrategy;
        this.numberOfSignificantValueDigits = numberOfSignificantValueDigits;
        this.lowestDiscernibleValue = lowestDiscernibleValue;
        this.highestTrackableValue = highestTrackableValue;
        this.overflowHandling = overflowHandling;
        this.snapshotCachingDurationMillis = snapshotCachingDurationMillis;
        this.predefinedPercentiles = predefinedPercentiles;
    }

    @Override
    public String toString() {
        return "HdrBuilder{" +
                "accumulationStrategy=" + accumulationStrategy +
                ", numberOfSignificantValueDigits=" + numberOfSignificantValueDigits +
                ", lowestDiscernibleValue=" + lowestDiscernibleValue +
                ", highestTrackableValue=" + highestTrackableValue +
                ", overflowHandling=" + overflowHandling +
                ", snapshotCachingDurationMillis=" + snapshotCachingDurationMillis +
                ", predefinedPercentiles=" + Arrays.toString(predefinedPercentiles.orElse(new double[0])) +
                '}';
    }

}
