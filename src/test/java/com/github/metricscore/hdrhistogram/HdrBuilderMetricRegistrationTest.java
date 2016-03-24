package com.github.metricscore.hdrhistogram;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.junit.Test;

import java.time.Duration;

import static org.junit.Assert.assertSame;

public class HdrBuilderMetricRegistrationTest {

    private HdrBuilder builder = new HdrBuilder().withLowestDiscernibleValue(3).withLowestDiscernibleValue(1000)
            .withHighestTrackableValue(3600000L, OverflowHandlingStrategy.REDUCE_TO_MAXIMUM)
            .withPredefinedPercentiles(new double[] {0.9, 0.95, 0.99})
            .withSnapshotCachingDuration(Duration.ofMinutes(1));

    private MetricRegistry registry = new MetricRegistry();

    @Test
    public void testBuildAndRegistrerHistogram() {
        Histogram historam = builder.buildAndRegisterHistogram(registry, "myhistogram");
        assertSame(historam, registry.histogram("myhistogram"));
    }

    @Test
    public void testBuildAndRegisterTimer() {
        Timer timer = builder.buildAndRegisterTimer(registry, "mytimer");
        assertSame(timer, registry.timer("mytimer"));
    }

}
