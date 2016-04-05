package com.github.metricscore.hdrhistogram;

import com.codahale.metrics.Histogram;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

/**
 * Created by vladimir.bukhtoyarov on 05.04.2016.
 */
public class ExpectedIntervalBetweenValueSamplesTest {

    @Test
    public void expectedIntervalBetweenValueSamples() {
        Histogram histogram = new HdrBuilder().withExpectedIntervalBetweenValueSamples(100).buildHistogram();
        for (int i = 1; i <= 100; i++) {
            histogram.update(i);
        }
        assertEquals(75.0, histogram.getSnapshot().get75thPercentile());
        assertEquals(99.0, histogram.getSnapshot().get99thPercentile());

        histogram.update(10000);
        assertEquals(5023.0, histogram.getSnapshot().get75thPercentile());
        assertEquals(9855.0, histogram.getSnapshot().get99thPercentile());
    }

}
