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
import com.github.rollingmetrics.retention.RetentionPolicy;
import org.junit.Test;

import java.time.Duration;

import static com.github.rollingmetrics.histogram.hdr.impl.ResetByChunksRollingHdrHistogramImpl.MAX_CHUNKS;
import static com.github.rollingmetrics.histogram.hdr.impl.ResetByChunksRollingHdrHistogramImpl.MIN_CHUNK_RESETTING_INTERVAL_MILLIS;
import static org.junit.Assert.fail;


public class RollingHdrHistogramBuilderArgumentCheckingTest {

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAllowNegativeSignificantDigits() {
        RetentionPolicy.uniform()
                .newRollingHdrHistogramBuilder()
                .withSignificantDigits(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAllowTooBigSignificantDigits() {
        RetentionPolicy.uniform()
                .newRollingHdrHistogramBuilder()
                .withSignificantDigits(6);
    }

    @Test
    public void shouldAllowSignificantDigitsBetweenZeroAndFive() {
        for (int digits = 0; digits < 6; digits++) {
            RetentionPolicy.uniform()
                    .newRollingHdrHistogramBuilder()
                    .withSignificantDigits(digits);
        }
    }

    @Test
    public void shouldNotAllowTooSmallSignificantDigitsLowestDiscernibleValue() {
        for (int value : new int[] {0, -1}) {
            try {
                RetentionPolicy.uniform()
                        .newRollingHdrHistogramBuilder()
                        .withLowestDiscernibleValue(value);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }
    }

    @Test
    public void shouldNotAllowTooSmallHighestTrackableValue() {
        for (int value : new int[] {0, -1, 1}) {
            try {
                RetentionPolicy.uniform()
                        .newRollingHdrHistogramBuilder()
                        .withHighestTrackableValue(value, OverflowResolver.PASS_THRU);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }
    }

    @Test(expected = IllegalStateException.class)
    public void shouldCheckThatHighestValueShouldBeTwoTimesGreaterThenLowest() {
        RetentionPolicy.uniform()
                .newRollingHdrHistogramBuilder()
                .withLowestDiscernibleValue(10)
                .withHighestTrackableValue(11, OverflowResolver.PASS_THRU)
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void shouldRequireHighestValueIfLowestSpecified() {
        RetentionPolicy.uniform()
                .newRollingHdrHistogramBuilder()
                .withLowestDiscernibleValue(10)
                .build();
    }

    @Test(expected = NullPointerException.class)
    public void shouldNotAllowNullOverflowHandlingStrategy() {
        RetentionPolicy.uniform()
                .newRollingHdrHistogramBuilder()
                .withHighestTrackableValue(42, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAllowNegativePercentiles() {
        RetentionPolicy.uniform()
                .newRollingHdrHistogramBuilder()
                .withPredefinedPercentiles(new double[] {0.1, -0.2, 0.4});
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAllowTooBigPercentiles() {
        RetentionPolicy.uniform()
                .newRollingHdrHistogramBuilder()
                .withPredefinedPercentiles(new double[] {0.1, 0.2, 1.1});
    }

    @Test(expected = NullPointerException.class)
    public void shouldNotAllowNullPercentiles() {
        RetentionPolicy.uniform()
                .newRollingHdrHistogramBuilder()
                .withPredefinedPercentiles(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAllowEmptyPercentiles() {
        RetentionPolicy.uniform()
                .newRollingHdrHistogramBuilder()
                .withPredefinedPercentiles(new double[0]);
    }

    @Test
    public void shouldSuccessfullyBuild() {
        RetentionPolicy.uniform()
                .withSnapshotCachingDuration(Duration.ofMinutes(1))
                .newRollingHdrHistogramBuilder()
                .withLowestDiscernibleValue(3).withLowestDiscernibleValue(1000)
                .withHighestTrackableValue(3600000L, OverflowResolver.REDUCE_TO_HIGHEST_TRACKABLE)
                .withPredefinedPercentiles(new double[] {0.9, 0.95, 0.99})
                .build();
    }

    @Test
    public void validateResetByChunksParametersTest() {
        RetentionPolicy.resetPeriodicallyByChunks(Duration.ofMillis(MIN_CHUNK_RESETTING_INTERVAL_MILLIS * MAX_CHUNKS), MAX_CHUNKS)
                .newRollingHdrHistogramBuilder()
                .build();

        try {
            RetentionPolicy.resetPeriodicallyByChunks(Duration.ofMillis(MIN_CHUNK_RESETTING_INTERVAL_MILLIS - 1), MAX_CHUNKS)
                .newRollingHdrHistogramBuilder()
                .build();
            fail("should disallow too short duration");
        } catch (IllegalArgumentException e) {}

        try {
            RetentionPolicy.resetPeriodicallyByChunks(Duration.ofMillis(MIN_CHUNK_RESETTING_INTERVAL_MILLIS), MAX_CHUNKS + 1)
                .newRollingHdrHistogramBuilder()
                .build();
            fail("should too many chunks");
        } catch (IllegalArgumentException e) {}
    }

}