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

package com.github.rollingmetrics.jmx.gcmonitor;

import com.github.rollingmetrics.gcmonitor.GarbageCollectorMXBeanMock;
import com.github.rollingmetrics.gcmonitor.GcMonitor;
import com.github.rollingmetrics.gcmonitor.MockTicker;
import com.github.rollingmetrics.gcmonitor.stat.Formatter;
import org.junit.Before;
import org.junit.Test;

import javax.management.openmbean.CompositeData;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;


public class JmxIntegrationTest {

    private MockTicker ticker = new MockTicker(0);

    private GarbageCollectorMXBeanMock young = new GarbageCollectorMXBeanMock("G1 Young Generation");
    private GarbageCollectorMXBeanMock old = new GarbageCollectorMXBeanMock("G1 Old Generation");

    GcMonitor monitor = GcMonitor.builder(Arrays.asList(young, old))
            .withTicker(ticker)
            .withHistogramSignificantDigits(3)
            .addRollingWindow("30sec", Duration.ofSeconds(30))
            .addRollingWindow("5min", Duration.ofMinutes(5))
            .build();

    GcStatistics stat = new GcStatistics(monitor);

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
        CompositeData monitorData = stat.getGcMonitorData();
        CompositeData collectorData = (CompositeData) monitorData.get(collector);
        CompositeData windowData = (CompositeData) collectorData.get(window);
        CompositeData histogramData = (CompositeData) windowData.get("pauseHistogram");

        assertEquals(count, (long) (Long) histogramData.get("count"));
        assertEquals(min, (long) (Long) histogramData.get("min"));
        assertEquals(max, (long) (Long) histogramData.get("max"));
    }

    private void checkPausePercentage(double pausePercentage, String collector, String window) {
        CompositeData monitorData = stat.getGcMonitorData();
        CompositeData collectorData = (CompositeData) monitorData.get(collector);
        CompositeData windowData = (CompositeData) collectorData.get(window);
        CompositeData utilizationData = (CompositeData) windowData.get("utilization");

        BigDecimal expectedPercentage = Formatter.roundToDigits(pausePercentage, 2);
        assertEquals(expectedPercentage, utilizationData.get("pausePercentage"));
    }

    private void checkMillisSpentInGc(long millis, String collector, String window) {
        CompositeData monitorData = stat.getGcMonitorData();
        CompositeData collectorData = (CompositeData) monitorData.get(collector);
        CompositeData windowData = (CompositeData) collectorData.get(window);
        CompositeData utilizationData = (CompositeData) windowData.get("utilization");

        assertEquals(millis, utilizationData.get("pauseDurationMillis"));
    }

}