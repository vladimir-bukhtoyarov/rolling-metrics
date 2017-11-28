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

package com.github.rollingmetrics.histogram.hdr.impl;

import com.github.rollingmetrics.histogram.OverflowResolver;
import org.HdrHistogram.Recorder;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;


public class RecorderSettings {

    public static int DEFAULT_NUMBER_OF_SIGNIFICANT_DIGITS = 2;
    public static double[] DEFAULT_PERCENTILES = new double[]{0.5, 0.75, 0.9, 0.95, 0.98, 0.99, 0.999};

    private int numberOfSignificantValueDigits = DEFAULT_NUMBER_OF_SIGNIFICANT_DIGITS;
    private Optional<Long> lowestDiscernibleValue = Optional.empty();
    private Optional<double[]> predefinedPercentiles = Optional.of(DEFAULT_PERCENTILES);
    private Optional<Long> highestTrackableValue = Optional.empty();
    private Optional<OverflowResolver> overflowResolver = Optional.empty();
    private Optional<Long> expectedIntervalBetweenValueSamples = Optional.empty();

    Optional<double[]> getPredefinedPercentiles() {
        return predefinedPercentiles;
    }

    Optional<Long> getExpectedIntervalBetweenValueSamples() {
        return expectedIntervalBetweenValueSamples;
    }

    Optional<Long> getHighestTrackableValue() {
        return highestTrackableValue;
    }

    Optional<OverflowResolver> getOverflowResolver() {
        return overflowResolver;
    }

    void validateParameters() {
        if (highestTrackableValue.isPresent() && lowestDiscernibleValue.isPresent() && highestTrackableValue.get() < 2L * lowestDiscernibleValue.get()) {
            throw new IllegalStateException("highestTrackableValue must be >= 2 * lowestDiscernibleValue");
        }
        if (lowestDiscernibleValue.isPresent() && !highestTrackableValue.isPresent()) {
            throw new IllegalStateException("lowestDiscernibleValue is specified but highestTrackableValue undefined");
        }
    }

    Recorder buildRecorder() {
        if (lowestDiscernibleValue.isPresent()) {
            return new Recorder(lowestDiscernibleValue.get(), highestTrackableValue.get(), numberOfSignificantValueDigits);
        }
        if (highestTrackableValue.isPresent()) {
            return new Recorder(highestTrackableValue.get(), numberOfSignificantValueDigits);
        }
        return new Recorder(numberOfSignificantValueDigits);
    }

    void setLowestDiscernibleValue(long lowestDiscernibleValue) {
        if (lowestDiscernibleValue < 1) {
            throw new IllegalArgumentException("lowestDiscernibleValue must be >= 1");
        }
        this.lowestDiscernibleValue = Optional.of(lowestDiscernibleValue);
    }

    void setSignificantDigits(int numberOfSignificantValueDigits) {
        if ((numberOfSignificantValueDigits < 0) || (numberOfSignificantValueDigits > 5)) {
            throw new IllegalArgumentException("numberOfSignificantValueDigits must be between 0 and 5");
        }
        this.numberOfSignificantValueDigits = numberOfSignificantValueDigits;
    }

    void setHighestTrackableValue(long highestTrackableValue, OverflowResolver overflowResolver) {
        if (highestTrackableValue < 2) {
            throw new IllegalArgumentException("highestTrackableValue must be >= 2");
        }
        if (overflowResolver == null) {
            throw new IllegalArgumentException("overflowResolver must not be null");
        }
        this.highestTrackableValue = Optional.of(highestTrackableValue);
        this.overflowResolver = Optional.of(overflowResolver);
    }

    void setExpectedIntervalBetweenValueSamples(long expectedIntervalBetweenValueSamples) {
        if (expectedIntervalBetweenValueSamples < 0) {
            throw new IllegalArgumentException("highestTrackableValue must be >= 0");
        }
        this.expectedIntervalBetweenValueSamples = Optional.of(expectedIntervalBetweenValueSamples);
    }

    void setPredefinedPercentiles(double[] predefinedPercentiles) {
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
    }

    void withoutSnapshotOptimization() {
        this.predefinedPercentiles = Optional.empty();
    }

    private static double[] copyAndSort(double[] predefinedPercentiles) {
        double[] sortedPercentiles = Arrays.copyOf(predefinedPercentiles, predefinedPercentiles.length);
        Arrays.sort(sortedPercentiles);
        return sortedPercentiles;
    }

}
