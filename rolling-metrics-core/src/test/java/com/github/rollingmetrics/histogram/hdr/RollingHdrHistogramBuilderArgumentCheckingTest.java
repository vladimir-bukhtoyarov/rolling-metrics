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
import org.junit.Test;

import java.time.Duration;

import static com.github.rollingmetrics.histogram.hdr.RollingHdrHistogramBuilder.MAX_CHUNKS;
import static com.github.rollingmetrics.histogram.hdr.RollingHdrHistogramBuilder.MIN_CHUNK_RESETTING_INTERVAL_MILLIS;
import static org.junit.Assert.fail;


public class RollingHdrHistogramBuilderArgumentCheckingTest {

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAllowNegativeSignificantDigits() {
        RollingHdrHistogram.builder().withSignificantDigits(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAllowTooBigSignificantDigits() {
        RollingHdrHistogram.builder().withSignificantDigits(6);
    }

    @Test
    public void shouldAllowSignificantDigitsBetweenZeroAndFive() {
        for (int digits = 0; digits < 6; digits++) {
            RollingHdrHistogram.builder().withSignificantDigits(digits);
        }
    }

    @Test
    public void shouldNotAllowTooSmallSignificantDigitsLowestDiscernibleValue() {
        for (int value : new int[] {0, -1}) {
            try {
                RollingHdrHistogram.builder().withLowestDiscernibleValue(value);
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
                RollingHdrHistogram.builder()
                        .withHighestTrackableValue(value, OverflowResolver.PASS_THRU);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }
    }

    @Test(expected = IllegalStateException.class)
    public void shouldCheckThatHighestValueShouldBeTwoTimesGreaterThenLowest() {
        RollingHdrHistogram.builder()
                .withLowestDiscernibleValue(10)
                .withHighestTrackableValue(11, OverflowResolver.PASS_THRU)
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void shouldRequireHighestValueIfLowestSpecified() {
        RollingHdrHistogram.builder()
                .withLowestDiscernibleValue(10)
                .build();
    }

    @Test(expected = NullPointerException.class)
    public void shouldNotAllowNullOverflowHandlingStrategy() {
        RollingHdrHistogram.builder()
                .withHighestTrackableValue(42, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAllowNegativeCachingDuration() {
        RollingHdrHistogram.builder()
                .withSnapshotCachingDuration(Duration.ofMillis(-1000));
    }

    @Test
    public void shouldAllowZeroCachingDuration() {
        RollingHdrHistogram.builder()
                .withSnapshotCachingDuration(Duration.ZERO);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAllowNegativePercentiles() {
        RollingHdrHistogram.builder()
                .withPredefinedPercentiles(new double[] {0.1, -0.2, 0.4});
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAllowTooBigPercentiles() {
        RollingHdrHistogram.builder()
                .withPredefinedPercentiles(new double[] {0.1, 0.2, 1.1});
    }

    @Test(expected = NullPointerException.class)
    public void shouldNotAllowNullPercentiles() {
        RollingHdrHistogram.builder()
                .withPredefinedPercentiles(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAllowEmptyPercentiles() {
        RollingHdrHistogram.builder()
                .withPredefinedPercentiles(new double[0]);
    }

    @Test
    public void shouldSuccessfullyBuild() {
        RollingHdrHistogram.builder()
                .withLowestDiscernibleValue(3).withLowestDiscernibleValue(1000)
                .withHighestTrackableValue(3600000L, OverflowResolver.REDUCE_TO_HIGHEST_TRACKABLE)
                .withPredefinedPercentiles(new double[] {0.9, 0.95, 0.99})
                .withSnapshotCachingDuration(Duration.ofMinutes(1))
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeResetPeriodShouldNotAllowedForResetReservoirPeriodically() {
        RollingHdrHistogram.builder()
                .resetReservoirPeriodically(Duration.ofMinutes(-5));
    }

    @Test(expected = IllegalArgumentException.class)
    public void zeroResetPeriodShouldNotAllowedForResetReservoirPeriodically() {
        RollingHdrHistogram.builder()
                .resetReservoirPeriodically(Duration.ZERO);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullExecutorShouldBeDeprecated() {
        RollingHdrHistogram.builder()
                .withBackgroundExecutor(null);
    }

    @Test
    public void validateResetByChunksParametersTest() {
        RollingHdrHistogram.builder()
                .resetReservoirPeriodicallyByChunks(Duration.ofMillis(MIN_CHUNK_RESETTING_INTERVAL_MILLIS * MAX_CHUNKS), MAX_CHUNKS);
        try {
            RollingHdrHistogram.builder()
                    .resetReservoirPeriodicallyByChunks(Duration.ofMillis(-1), 2);
            fail("should disallow negative duration");
        } catch (IllegalArgumentException e) {}

        try {
            RollingHdrHistogram.builder()
                    .resetReservoirPeriodicallyByChunks(Duration.ofMillis(MIN_CHUNK_RESETTING_INTERVAL_MILLIS - 1), MAX_CHUNKS);
            fail("should disallow too short duration");
        } catch (IllegalArgumentException e) {}

        try {
            RollingHdrHistogram.builder()
                    .resetReservoirPeriodicallyByChunks(Duration.ofMillis(MIN_CHUNK_RESETTING_INTERVAL_MILLIS), MAX_CHUNKS + 1);
            fail("should too many chunks");
        } catch (IllegalArgumentException e) {}

        try {
            RollingHdrHistogram.builder()
                    .resetReservoirPeriodicallyByChunks(Duration.ofMillis(MIN_CHUNK_RESETTING_INTERVAL_MILLIS), 0);
            fail("should check that chunks >= 1");
        } catch (IllegalArgumentException e) {}
    }

}