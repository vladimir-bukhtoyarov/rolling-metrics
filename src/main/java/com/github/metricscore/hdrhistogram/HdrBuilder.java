package com.github.metricscore.hdrhistogram;

import java.util.Optional;

public class HdrBuilder {

    public static int DEFAULT_NUMBER_OF_SIGNIFICANT_DIGITS = 2;
    public static Optional<Long> DEFAULT_LOWEST_DISCERNIBLE_VALUE = Optional.of(1L);
    public static Optional<OverflowHandlingStrategy> DEFAULT_OVERFLOW_HANDLING = Optional.of(OverflowHandlingStrategy.REDUCE_TO_MAXIMUM);

    private int numberOfSignificantValueDigits;
    private Optional<Long> lowestDiscernibleValue;
    private Optional<Long> highestTrackableValue;
    private Optional<OverflowHandlingStrategy> overflowHandling;

    public static HdrBuilder builder() {
        return new HdrBuilder(DEFAULT_NUMBER_OF_SIGNIFICANT_DIGITS, Optional.empty(), Optional.empty(), Optional.empty());
    }

    public HdrBuilder withSignificantDigits(int numberOfSignificantValueDigits) {
        this.numberOfSignificantValueDigits = numberOfSignificantValueDigits;
        return this;
    }

    public HdrBuilder withLowestDiscernibleValue(long lowestDiscernibleValue) {
        this.lowestDiscernibleValue = Optional.of(lowestDiscernibleValue);
        return this;
    }

    public HdrBuilder withHighestTrackableValue(long highestTrackableValue) {
        this.highestTrackableValue = Optional.of(highestTrackableValue);
        if (!lowestDiscernibleValue.isPresent()) {
            lowestDiscernibleValue = DEFAULT_LOWEST_DISCERNIBLE_VALUE;
        }
        if (!overflowHandling.isPresent()) {
            overflowHandling = DEFAULT_OVERFLOW_HANDLING;
        }
        return this;
    }

    private HdrBuilder(int numberOfSignificantValueDigits, Optional<Long> lowestDiscernibleValue, Optional<Long> highestTrackableValue, Optional<OverflowHandlingStrategy> overflowHandling) {
        this.numberOfSignificantValueDigits = numberOfSignificantValueDigits;
        this.lowestDiscernibleValue = lowestDiscernibleValue;
        this.highestTrackableValue = highestTrackableValue;
        this.overflowHandling = overflowHandling;
    }

}
