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

package com.github.rollingmetrics.histogram;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.github.rollingmetrics.histogram.HdrBuilder;
import com.github.rollingmetrics.histogram.OverflowResolver;
import org.junit.Test;

import java.time.Duration;

import static org.junit.Assert.assertSame;

public class HdrBuilderMetricRegistrationTest {

    private HdrBuilder builder = new HdrBuilder().withLowestDiscernibleValue(3).withLowestDiscernibleValue(1000)
            .withHighestTrackableValue(3600000L, OverflowResolver.REDUCE_TO_HIGHEST_TRACKABLE)
            .withPredefinedPercentiles(new double[] {0.9, 0.95, 0.99})
            .withSnapshotCachingDuration(Duration.ofMinutes(1));

    private MetricRegistry registry = new MetricRegistry();

    @Test
    public void testBuildAndRegisterHistogram() {
        Histogram historam = builder.buildAndRegisterHistogram(registry, "myhistogram");
        assertSame(historam, registry.histogram("myhistogram"));
    }

    @Test
    public void testBuildAndRegisterTimer() {
        Timer timer = builder.buildAndRegisterTimer(registry, "mytimer");
        assertSame(timer, registry.timer("mytimer"));
    }

}
