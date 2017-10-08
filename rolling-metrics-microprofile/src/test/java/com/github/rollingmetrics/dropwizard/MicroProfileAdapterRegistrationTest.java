/*
 *
 *  Copyright 2017 Vladimir Bukhtoyarov
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

package com.github.rollingmetrics.dropwizard;

import com.github.rollingmetrics.histogram.OverflowResolver;
import com.github.rollingmetrics.histogram.hdr.RollingHdrHistogram;
import com.github.rollingmetrics.histogram.hdr.RollingHdrHistogramBuilder;
import com.github.rollingmetrics.microprofile.MicroProfileAdapter;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;
import org.junit.Test;
import org.mockito.Mockito;

import java.time.Duration;

public class MicroProfileAdapterRegistrationTest {

    private RollingHdrHistogramBuilder builder = RollingHdrHistogram.builder().
            withLowestDiscernibleValue(3).withLowestDiscernibleValue(1000)
            .withHighestTrackableValue(3600000L, OverflowResolver.REDUCE_TO_HIGHEST_TRACKABLE)
            .withPredefinedPercentiles(new double[] {0.9, 0.95, 0.99})
            .withSnapshotCachingDuration(Duration.ofMinutes(1));

    private MetricRegistry registry = Mockito.mock(MetricRegistry.class);
    private Meter meter = Mockito.mock(Meter.class);

    @Test
    public void testBuildAndRegisterHistogram() {
        RollingHdrHistogram hdrHistogram = builder.build();
        Histogram historam = MicroProfileAdapter.convertToHistogramAndRegister(hdrHistogram, registry, "myhistogram");
        Mockito.verify(registry).register("myhistogram", historam);
    }

    @Test
    public void testBuildAndRegisterTimer() {
        RollingHdrHistogram hdrHistogram = builder.build();
        Timer timer = MicroProfileAdapter.convertToTimerAndRegister(hdrHistogram, meter, registry, "mytimer");
        Mockito.verify(registry).register("mytimer", timer);
    }

}
