package com.github.metricscore.hdrhistogram;

import java.util.Arrays;
import java.util.Optional;

public class HdrBuilder {

    public static int DEFAULT_NUMBER_OF_SIGNIFICANT_DIGITS = 2;
    public static AccumulationStrategy DEFAULT_ACCUMULATION_STRATEGY = AccumulationStrategy.RESET_ON_SNAPSHOT;
    public static double[] DEFAULT_PERCENTILES = new double[] {0.5, 0.75, 0.95, 0.98, 0.99, 0.999};

    private AccumulationStrategy accumulationStrategy;
    private int numberOfSignificantValueDigits = DEFAULT_NUMBER_OF_SIGNIFICANT_DIGITS;
    private Optional<Long> lowestDiscernibleValue;
    private Optional<Long> highestTrackableValue;
    private Optional<OverflowHandlingStrategy> overflowHandling;
    private Optional<Long> snapshotCachingDurationMillis;
    private Optional<double[]> predefinedPercentiles;

    public HdrBuilder() {
        this(DEFAULT_ACCUMULATION_STRATEGY, DEFAULT_NUMBER_OF_SIGNIFICANT_DIGITS, Optional.of(DEFAULT_PERCENTILES), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public HdrBuilder withSignificantDigits(int numberOfSignificantValueDigits) {
        this.numberOfSignificantValueDigits = numberOfSignificantValueDigits;
        return this;
    }

    public HdrBuilder withLowestDiscernibleValue(long lowestDiscernibleValue) {
        this.lowestDiscernibleValue = Optional.of(lowestDiscernibleValue);
        return this;
    }

    public HdrBuilder withHighestTrackableValue(long highestTrackableValue, OverflowHandlingStrategy overflowHandling) {
        this.highestTrackableValue = Optional.of(highestTrackableValue);
        this.overflowHandling = Optional.of(overflowHandling);
        return this;
    }

    public HdrBuilder withSnapshotCachingDurationMillis(long snapshotCachingDurationMillis) {
        this.snapshotCachingDurationMillis = Optional.of(snapshotCachingDurationMillis);
        return this;
    }

    public HdrBuilder withPredefinedPercentiles(double[] predefinedPercentiles) {
        double[] sortedPercentiles = copyAndSort(predefinedPercentiles);
        this.predefinedPercentiles = Optional.of(sortedPercentiles);
        return this;
    }

    private double[] copyAndSort(double[] predefinedPercentiles) {
        double[] sortedPercentiles = Arrays.copyOf(predefinedPercentiles, predefinedPercentiles.length);
        Arrays.sort(sortedPercentiles);
        return sortedPercentiles;
    }

    public HdrReservoir buildHdrReservoir() {
        validate(accumulationStrategy, numberOfSignificantValueDigits, lowestDiscernibleValue, highestTrackableValue,
                overflowHandling, snapshotCachingDurationMillis, predefinedPercentiles);
        return new HdrReservoir(accumulationStrategy, numberOfSignificantValueDigits, lowestDiscernibleValue,
                highestTrackableValue, overflowHandling, snapshotCachingDurationMillis, predefinedPercentiles, WallClock.INSTANCE);
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

    private void validate(AccumulationStrategy accumulationStrategy, int numberOfSignificantValueDigits,
                          Optional<Long> lowestDiscernibleValue, Optional<Long> highestTrackableValue,
                          Optional<OverflowHandlingStrategy> overflowHandling,
                          Optional<Long> cachingDurationMillis, Optional<double[]> predefinedPercentiles) {
        // TODO
    }

}
