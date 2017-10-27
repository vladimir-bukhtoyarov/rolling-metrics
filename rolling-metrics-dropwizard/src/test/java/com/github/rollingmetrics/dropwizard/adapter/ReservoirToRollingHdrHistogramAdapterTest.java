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

package com.github.rollingmetrics.dropwizard.adapter;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import com.github.rollingmetrics.dropwizard.Dropwizard;
import com.github.rollingmetrics.histogram.hdr.RollingHdrHistogram;
import com.github.rollingmetrics.histogram.hdr.RollingSnapshot;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertEquals;

/**
 * Created by vladimir.bukhtoyarov on 11.09.2017.
 */
public class ReservoirToRollingHdrHistogramAdapterTest {

    RollingHdrHistogram rollingHistogram = RollingHdrHistogram.builder()
            .withSignificantDigits(4).build();
    Histogram histogram = Dropwizard.toHistogram(rollingHistogram);
    Timer timer = Dropwizard.toTimer(rollingHistogram);

    @Before
    public void initValues() {
        for (int i = 1; i <= 1000; i++) {
            histogram.update(i);
            timer.update(i, TimeUnit.NANOSECONDS);
        }
    }

    @Test
    public void testTimerConversion() {
        compareSnapshots(rollingHistogram.getSnapshot(), timer.getSnapshot());
        assertEquals(1000, timer.getCount());
    }

    @Test
    public void shouldSkipNegativeLatencyWhenUpdateTimer() {
        timer.update(-10, TimeUnit.NANOSECONDS);
        assertEquals(1000, timer.getCount());
        assertEquals(1, timer.getSnapshot().getMin());
    }

    @Test
    public void testHistogramConversion() {
        compareSnapshots(rollingHistogram.getSnapshot(), histogram.getSnapshot());
        assertEquals(1000, histogram.getCount());
    }

    private void compareSnapshots(RollingSnapshot rollingSnapshot, Snapshot dropwizzardSnapshot) {
        assertEquals(rollingSnapshot.getMax(), dropwizzardSnapshot.getMax());
        assertEquals(rollingSnapshot.getMin(), dropwizzardSnapshot.getMin());
        assertEquals(rollingSnapshot.getMedian(), dropwizzardSnapshot.getMedian());
        assertEquals(rollingSnapshot.getMean(), dropwizzardSnapshot.getMean());
        assertEquals(rollingSnapshot.getValue(0.75), dropwizzardSnapshot.get75thPercentile());
        assertEquals(rollingSnapshot.getValue(0.75), dropwizzardSnapshot.getValue(0.75));
    }

}