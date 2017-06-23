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
import org.HdrHistogram.Recorder;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;


public class RecorderSettings {

    private final int numberOfSignificantValueDigits;
    private final Optional<Long> lowestDiscernibleValue;
    private final Optional<double[]> predefinedPercentiles;
    private final Optional<Long> highestTrackableValue;
    private final Optional<OverflowResolver> overflowResolver;
    private final Optional<Long> expectedIntervalBetweenValueSamples;

    public RecorderSettings(int numberOfSignificantValueDigits,
                            Optional<Long> lowestDiscernibleValue,
                            Optional<double[]> predefinedPercentiles,
                            Optional<Long> highestTrackableValue,
                            Optional<OverflowResolver> overflowResolver,
                            Optional<Long> expectedIntervalBetweenValueSamples) {
        this.numberOfSignificantValueDigits = numberOfSignificantValueDigits;
        this.lowestDiscernibleValue = lowestDiscernibleValue;
        this.predefinedPercentiles = predefinedPercentiles;
        this.highestTrackableValue = highestTrackableValue;
        this.overflowResolver = overflowResolver;
        this.expectedIntervalBetweenValueSamples = expectedIntervalBetweenValueSamples;
    }

    public Optional<double[]> getPredefinedPercentiles() {
        return predefinedPercentiles;
    }

    public Optional<Long> getExpectedIntervalBetweenValueSamples() {
        return expectedIntervalBetweenValueSamples;
    }

    public Optional<Long> getHighestTrackableValue() {
        return highestTrackableValue;
    }

    public Optional<OverflowResolver> getOverflowResolver() {
        return overflowResolver;
    }

    public void validateParameters() {
        if (highestTrackableValue.isPresent() && lowestDiscernibleValue.isPresent() && highestTrackableValue.get() < 2L * lowestDiscernibleValue.get()) {
            throw new IllegalStateException("highestTrackableValue must be >= 2 * lowestDiscernibleValue");
        }

        if (lowestDiscernibleValue.isPresent() && !highestTrackableValue.isPresent()) {
            throw new IllegalStateException("lowestDiscernibleValue is specified but highestTrackableValue undefined");
        }
    }

    public Recorder buildRecorder() {
        if (lowestDiscernibleValue.isPresent()) {
            return new Recorder(lowestDiscernibleValue.get(), highestTrackableValue.get(), numberOfSignificantValueDigits);
        }
        if (highestTrackableValue.isPresent()) {
            return new Recorder(highestTrackableValue.get(), numberOfSignificantValueDigits);
        }
        return new Recorder(numberOfSignificantValueDigits);
    }

    public RecorderSettings withLowestDiscernibleValue(long lowestDiscernibleValue) {
        if (lowestDiscernibleValue < 1) {
            throw new IllegalArgumentException("lowestDiscernibleValue must be >= 1");
        }
        return new RecorderSettings(
                this.numberOfSignificantValueDigits,
                Optional.of(lowestDiscernibleValue),
                this.predefinedPercentiles,
                this.highestTrackableValue,
                this.overflowResolver,
                this.expectedIntervalBetweenValueSamples
        );
    }

    public RecorderSettings withSignificantDigits(int numberOfSignificantValueDigits) {
        if ((numberOfSignificantValueDigits < 0) || (numberOfSignificantValueDigits > 5)) {
            throw new IllegalArgumentException("numberOfSignificantValueDigits must be between 0 and 5");
        }
        return new RecorderSettings(
                numberOfSignificantValueDigits,
                this.lowestDiscernibleValue,
                this.predefinedPercentiles,
                this.highestTrackableValue,
                this.overflowResolver,
                this.expectedIntervalBetweenValueSamples
        );
    }

    public RecorderSettings withHighestTrackableValue(long highestTrackableValue, OverflowResolver overflowResolver) {
        if (highestTrackableValue < 2) {
            throw new IllegalArgumentException("highestTrackableValue must be >= 2");
        }
        return new RecorderSettings(
                this.numberOfSignificantValueDigits,
                this.lowestDiscernibleValue,
                this.predefinedPercentiles,
                Optional.of(highestTrackableValue),
                Optional.of(overflowResolver),
                this.expectedIntervalBetweenValueSamples
        );
    }

    public RecorderSettings withExpectedIntervalBetweenValueSamples(long expectedIntervalBetweenValueSamples) {
        return new RecorderSettings(
                this.numberOfSignificantValueDigits,
                this.lowestDiscernibleValue,
                this.predefinedPercentiles,
                this.highestTrackableValue,
                this.overflowResolver,
                Optional.of(expectedIntervalBetweenValueSamples)
        );
    }

    public RecorderSettings withPredefinedPercentiles(double[] predefinedPercentiles) {
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

        return new RecorderSettings(
                this.numberOfSignificantValueDigits,
                this.lowestDiscernibleValue,
                Optional.of(sortedPercentiles),
                this.highestTrackableValue,
                this.overflowResolver,
                this.expectedIntervalBetweenValueSamples
        );
    }

    public RecorderSettings withoutSnapshotOptimization() {
        return new RecorderSettings(
                this.numberOfSignificantValueDigits,
                this.lowestDiscernibleValue,
                Optional.empty(),
                this.highestTrackableValue,
                this.overflowResolver,
                this.expectedIntervalBetweenValueSamples
        );
    }

    private static double[] copyAndSort(double[] predefinedPercentiles) {
        double[] sortedPercentiles = Arrays.copyOf(predefinedPercentiles, predefinedPercentiles.length);
        Arrays.sort(sortedPercentiles);
        return sortedPercentiles;
    }

}
