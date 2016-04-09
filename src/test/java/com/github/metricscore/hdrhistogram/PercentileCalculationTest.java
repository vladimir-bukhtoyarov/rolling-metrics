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

package com.github.metricscore.hdrhistogram;


import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Snapshot;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.HistogramIterationValue;
import org.junit.Test;

import java.util.Arrays;
import java.util.function.Function;

import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertEquals;

public class PercentileCalculationTest {

    private Function<Reservoir, Snapshot> snapshotTaker = reservoir -> {
        for (int i = 1; i <= 100000; i++) {
            reservoir.update(i);
        }
        return reservoir.getSnapshot();
    };

    private Histogram createEquivalentHistogram() {
        Histogram histogram = new Histogram(2);
        for (int i = 1; i <= 100000; i++) {
            histogram.recordValue(i);
        }
        return histogram;
    }

    @Test
    public void testSmartSnapshotCalculation() {
        double[] predefinedPercentiles = {0.5, 0.6, 0.75, 0.9, 0.95, 0.98, 0.99, 0.999};
        Reservoir reservoir = new HdrBuilder().withPredefinedPercentiles(predefinedPercentiles).buildReservoir();
        Snapshot snapshot = snapshotTaker.apply(reservoir);

        Histogram hdrHistogram = createEquivalentHistogram();
        assertEquals(hdrHistogram.getStdDeviation(), snapshot.getStdDev());
        assertEquals(hdrHistogram.getMinValue(), snapshot.getMin());
        assertEquals(hdrHistogram.getMean(), snapshot.getMean());
        assertEquals(hdrHistogram.getValueAtPercentile(50.0), (long) snapshot.getValue(0.42)); // do not defined percentile should be rounded up to first defined
        assertEquals(hdrHistogram.getValueAtPercentile(50.0), (long) snapshot.getMedian());
        assertEquals(hdrHistogram.getMaxValue(), snapshot.getMax());
        assertEquals(hdrHistogram.getValueAtPercentile(60.0), (long) snapshot.getValue(0.6));
        assertEquals(hdrHistogram.getValueAtPercentile(75.0), (long) snapshot.get75thPercentile());
        assertEquals(hdrHistogram.getValueAtPercentile(90.0), (long) snapshot.getValue(0.8)); // do not defined percentile should be rounded up to first defined
        assertEquals(hdrHistogram.getValueAtPercentile(90.0), (long) snapshot.getValue(0.9));
        assertEquals(hdrHistogram.getValueAtPercentile(95.0), (long) snapshot.getValue(0.94)); // do not defined percentile should be rounded up to first defined
        assertEquals(hdrHistogram.getValueAtPercentile(95.0), (long) snapshot.get95thPercentile());
        assertEquals(hdrHistogram.getValueAtPercentile(98.0), (long) snapshot.get98thPercentile());
        assertEquals(hdrHistogram.getValueAtPercentile(99.0), (long) snapshot.get99thPercentile());
        assertEquals(hdrHistogram.getValueAtPercentile(99.9), (long) snapshot.get999thPercentile());
        assertEquals(hdrHistogram.getMaxValue(), (long) snapshot.getValue(0.9999));

        assertEquals(predefinedPercentiles.length, snapshot.size());

        assertTrue(Arrays.equals(
                snapshot.getValues(),
                new long[] {
                        hdrHistogram.getValueAtPercentile(50.0),
                        hdrHistogram.getValueAtPercentile(60.0),
                        hdrHistogram.getValueAtPercentile(75.0),
                        hdrHistogram.getValueAtPercentile(90.0),
                        hdrHistogram.getValueAtPercentile(95.0),
                        hdrHistogram.getValueAtPercentile(98.0),
                        hdrHistogram.getValueAtPercentile(99.0),
                        hdrHistogram.getValueAtPercentile(99.9),
                }
        ));
    }

    @Test
    public void testFullSnapshotCalculation() {
        Reservoir reservoir = new HdrBuilder().withoutSnapshotOptimization().buildReservoir();
        Snapshot snapshot = snapshotTaker.apply(reservoir);

        Histogram hdrHistogram = createEquivalentHistogram();
        assertEquals(hdrHistogram.getStdDeviation(), snapshot.getStdDev());
        assertEquals(hdrHistogram.getMinValue(), snapshot.getMin());
        assertEquals(hdrHistogram.getMean(), snapshot.getMean());
        assertEquals(hdrHistogram.getValueAtPercentile(50.0), (long) snapshot.getMedian());
        assertEquals(hdrHistogram.getMaxValue(), snapshot.getMax());
        assertEquals(hdrHistogram.getValueAtPercentile(60.0), (long) snapshot.getValue(0.6));
        assertEquals(hdrHistogram.getValueAtPercentile(75.0), (long) snapshot.get75thPercentile());
        assertEquals(hdrHistogram.getValueAtPercentile(80.0), (long) snapshot.getValue(0.8));
        assertEquals(hdrHistogram.getValueAtPercentile(90.0), (long) snapshot.getValue(0.9));
        assertEquals(hdrHistogram.getValueAtPercentile(94.0), (long) snapshot.getValue(0.94));
        assertEquals(hdrHistogram.getValueAtPercentile(95.0), (long) snapshot.get95thPercentile());
        assertEquals(hdrHistogram.getValueAtPercentile(98.0), (long) snapshot.get98thPercentile());
        assertEquals(hdrHistogram.getValueAtPercentile(99.0), (long) snapshot.get99thPercentile());
        assertEquals(hdrHistogram.getValueAtPercentile(99.9), (long) snapshot.get999thPercentile());

        assertEquals(hdrHistogram.getTotalCount(), snapshot.size());

        int i = 0;
        long[] values = snapshot.getValues();
        for (HistogramIterationValue value : hdrHistogram.recordedValues()) {
            assertEquals(value.getValueIteratedTo(), values[i++]);
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void sizeMethodShouldBeUnsupported() {
        new HdrBuilder().buildReservoir().size();
    }

}
