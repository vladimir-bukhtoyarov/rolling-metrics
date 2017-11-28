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
import org.HdrHistogram.Recorder;
import org.junit.Test;

import java.time.Duration;

import static org.junit.Assert.assertEquals;


public class EstimationFootprintInBytesTest {

    private int histogramEquivalentEstimate = new Recorder(10, 3600, 3).getIntervalHistogram().getEstimatedFootprintInBytes();

    @Test
    public void testEstimationFootprintInBytes() {
        assertEquals(histogramEquivalentEstimate * 3, footprint(RetentionPolicy.uniform()));
        assertEquals(histogramEquivalentEstimate * 2, footprint(RetentionPolicy.resetOnSnapshot()));
        assertEquals(histogramEquivalentEstimate * 7, footprint(RetentionPolicy.resetPeriodically(Duration.ofMinutes(1))));
        assertEquals(histogramEquivalentEstimate * (10 + 6 + 1), footprint(RetentionPolicy.resetPeriodicallyByChunks(Duration.ofMinutes(1), 10)));
    }

    private int footprint(RetentionPolicy policy) {
        return policy.newRollingHdrHistogramBuilder()
                .withHighestTrackableValue(3600, OverflowResolver.REDUCE_TO_HIGHEST_TRACKABLE)
                .withLowestDiscernibleValue(10)
                .withSignificantDigits(3)
                .getEstimatedFootprintInBytes();
    }

}
