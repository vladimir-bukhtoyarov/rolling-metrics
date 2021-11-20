/*
 *  Copyright 2017 Vladimir Bukhtoyarov
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.github.rollingmetrics.dropwizard.adapter;


import com.codahale.metrics.*;
import com.github.rollingmetrics.dropwizard.adapter.GcMonitorMetricSet;
import com.github.rollingmetrics.gcmonitor.GarbageCollectorMXBeanMock;
import com.github.rollingmetrics.gcmonitor.GcMonitor;
import com.github.rollingmetrics.gcmonitor.MockTicker;
import com.github.rollingmetrics.gcmonitor.stat.Formatter;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class GcMonitorMetricSetTest {

    private MockTicker ticker = new MockTicker(0);

    private GarbageCollectorMXBeanMock young = new GarbageCollectorMXBeanMock("G1 Young Generation");
    private GarbageCollectorMXBeanMock old = new GarbageCollectorMXBeanMock("G1 Old Generation");

    GcMonitor monitor = GcMonitor.builder(Arrays.asList(young, old))
            .withTicker(ticker)
            .withHistogramSignificantDigits(3)
            .addRollingWindow("30sec", Duration.ofSeconds(30))
            .addRollingWindow("5min", Duration.ofMinutes(5))
            .build();

    MetricSet metricSet = GcMonitorMetricSet.toMetricSet("gc", monitor);
    MetricRegistry registry = new MetricRegistry(); {
        registry.registerAll(metricSet);
    }

    @Before
    public void init() {
        monitor.start();
    }

    @Test
    public void testAdapter() {
        ticker.setCurrentTimeMillis(10_000);
        young.addFakeCollection(100);
        young.addFakeCollection(200);

        old.addFakeCollection(300);
        old.addFakeCollection(400);

        checkMillisSpentInGc(1000, "AggregatedCollector", "30sec");
        checkMillisSpentInGc(1000, "AggregatedCollector", "5min");
        checkMillisSpentInGc(1000, "AggregatedCollector", "uniform");

        checkMillisSpentInGc(300, "G1-Young-Generation", "30sec");
        checkMillisSpentInGc(300, "G1-Young-Generation", "5min");
        checkMillisSpentInGc(300, "G1-Young-Generation", "uniform");

        checkMillisSpentInGc(700, "G1-Old-Generation", "30sec");
        checkMillisSpentInGc(700, "G1-Old-Generation", "5min");
        checkMillisSpentInGc(700, "G1-Old-Generation", "uniform");

        checkPausePercentage(10.0, "AggregatedCollector", "30sec");
        checkPausePercentage(10.0, "AggregatedCollector", "5min");
        checkPausePercentage(10.0, "AggregatedCollector", "uniform");

        checkPausePercentage(3.0, "G1-Young-Generation", "30sec");
        checkPausePercentage(3.0, "G1-Young-Generation", "5min");
        checkPausePercentage(3.0, "G1-Young-Generation", "uniform");

        checkPausePercentage(7.0, "G1-Old-Generation", "30sec");
        checkPausePercentage(7.0, "G1-Old-Generation", "5min");
        checkPausePercentage(7.0, "G1-Old-Generation", "uniform");

        checkHistogram(4, 100, 400, "AggregatedCollector", "30sec");
        checkHistogram(4, 100, 400, "AggregatedCollector", "5min");
        checkHistogram(4, 100, 400, "AggregatedCollector", "uniform");

        checkHistogram(2, 100, 200, "G1-Young-Generation", "30sec");
        checkHistogram(2, 100, 200, "G1-Young-Generation", "5min");
        checkHistogram(2, 100, 200, "G1-Young-Generation", "uniform");

        checkHistogram(2, 300, 400, "G1-Old-Generation", "30sec");
        checkHistogram(2, 300, 400, "G1-Old-Generation", "5min");
        checkHistogram(2, 300, 400, "G1-Old-Generation", "uniform");
    }

    private void checkHistogram(long count, long min, long max, String collector, String window) {
        String name = "gc." + collector + "." + window + ".pauseLatencyMillis";
        Histogram histogram = registry.histogram(name);
        Snapshot snapshot = histogram.getSnapshot();
        assertEquals(count, histogram.getCount());
        assertEquals(min, snapshot.getMin());
        assertEquals(max, snapshot.getMax());
    }

    private void checkPausePercentage(double pausePercentage, String collector, String window) {
        String name = "gc." + collector + "." + window + ".pausePercentage";
        Gauge<BigDecimal> gauge = registry.getGauges().get(name);
        BigDecimal expectedPercentage = Formatter.roundToDigits(pausePercentage, 2);
        assertEquals(expectedPercentage, gauge.getValue());
    }

    private void checkMillisSpentInGc(long millis, String collector, String window) {
        String name = "gc." + collector + "." + window + ".millisSpentInPause";
        Gauge<Long> gauge = registry.getGauges().get(name);
        assertEquals(millis, gauge.getValue().longValue());
    }

}