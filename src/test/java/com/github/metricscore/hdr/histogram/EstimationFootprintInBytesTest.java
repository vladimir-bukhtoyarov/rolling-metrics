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

package com.github.metricscore.hdr.histogram;
import com.github.metricscore.hdr.histogram.HdrBuilder;
import com.github.metricscore.hdr.histogram.OverflowResolver;
import org.HdrHistogram.Recorder;
import org.junit.Test;

import java.time.Duration;

import static org.junit.Assert.assertEquals;

public class EstimationFootprintInBytesTest {

    private HdrBuilder builder = new HdrBuilder().withHighestTrackableValue(3600, OverflowResolver.REDUCE_TO_HIGHEST_TRACKABLE)
            .withLowestDiscernibleValue(10)
            .withSignificantDigits(3);

    private int histogramEquivalentEstimate = new Recorder(10, 3600, 3).getIntervalHistogram().getEstimatedFootprintInBytes();

    @Test
    public void testEstimationFootprintInBytes() {
        System.out.println(histogramEquivalentEstimate * (61 + 5));
        assertEquals(histogramEquivalentEstimate * 3, builder.neverResetReservoir().getEstimatedFootprintInBytes());
        assertEquals(histogramEquivalentEstimate * 2, builder.resetReservoirOnSnapshot().getEstimatedFootprintInBytes());
        assertEquals(histogramEquivalentEstimate * 3, builder.resetReservoirPeriodically(Duration.ofMinutes(1)).getEstimatedFootprintInBytes());
        assertEquals(histogramEquivalentEstimate * (10 + 7), builder.resetReservoirByChunks(Duration.ofMinutes(1), 10).getEstimatedFootprintInBytes());
        assertEquals(histogramEquivalentEstimate * (10 + 7), builder.resetReservoirByChunks(Duration.ofMinutes(1), 10, true).getEstimatedFootprintInBytes());
        assertEquals(histogramEquivalentEstimate * (10 + 5), builder.resetReservoirByChunks(Duration.ofMinutes(1), 10, false).getEstimatedFootprintInBytes());
    }

}
