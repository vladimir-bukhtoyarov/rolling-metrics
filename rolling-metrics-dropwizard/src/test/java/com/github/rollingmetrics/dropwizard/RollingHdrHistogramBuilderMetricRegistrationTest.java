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

package com.github.rollingmetrics.dropwizard;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.github.rollingmetrics.dropwizard.adapter.DropwizardAdapter;
import com.github.rollingmetrics.histogram.OverflowResolver;
import com.github.rollingmetrics.histogram.hdr.RollingHdrHistogram;
import com.github.rollingmetrics.histogram.hdr.RollingHdrHistogramBuilder;
import org.junit.Test;

import java.time.Duration;

import static org.junit.Assert.assertSame;

public class RollingHdrHistogramBuilderMetricRegistrationTest {

    private RollingHdrHistogramBuilder builder = RollingHdrHistogram.builder().
            withLowestDiscernibleValue(3).withLowestDiscernibleValue(1000)
            .withHighestTrackableValue(3600000L, OverflowResolver.REDUCE_TO_HIGHEST_TRACKABLE)
            .withPredefinedPercentiles(new double[] {0.9, 0.95, 0.99})
            .withSnapshotCachingDuration(Duration.ofMinutes(1));

    private MetricRegistry registry = new MetricRegistry();

    @Test
    public void testBuildAndRegisterHistogram() {
        RollingHdrHistogram hdrHistogram = builder.build();
        Histogram historam = DropwizardAdapter.convertToHistogramAndRegister(hdrHistogram, registry, "myhistogram");
        assertSame(historam, registry.histogram("myhistogram"));
    }

    @Test
    public void testBuildAndRegisterTimer() {
        RollingHdrHistogram hdrHistogram = builder.build();
        Timer timer = DropwizardAdapter.convertToTimerAndRegister(hdrHistogram, registry, "mytimer");
        assertSame(timer, registry.timer("mytimer"));
    }

}
