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

package io.github.gcmonitor;

import io.github.gcmonitor.stat.GcMonitorSnapshot;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class SnapshotCalculationTest {

    private MockTicker ticker = new MockTicker(0);

    private GarbageCollectorMXBeanMock young = new GarbageCollectorMXBeanMock("G1 Young Generation");
    private GarbageCollectorMXBeanMock old = new GarbageCollectorMXBeanMock("G1 Old Generation");

    GcMonitor monitor = GcMonitor.builder(Arrays.asList(young, old))
            .withTicker(ticker)
            .withHistogramSignificantDigits(3)
            .addRollingWindow("30sec", Duration.ofSeconds(30))
            .addRollingWindow("5min", Duration.ofMinutes(5))
            .build();

    @Before
    public void init() {
        monitor.start();
    }

    @Test
    public void testSnapshot() {
        ticker.setCurrentTimeMillis(1000);
        young.addFakeCollection(100);
        GcMonitorSnapshot snapshot = monitor.getSnapshot();
        assertEquals(10.0, snapshot.getCollectorWindowSnapshot("G1-Young-Generation", "30sec").getPercentageSpentInGc(), 0.001);
        assertEquals(100.0, snapshot.getCollectorWindowSnapshot("G1-Young-Generation", "30sec").getPauseHistogramSnapshot().getMin(), 0.001);
        assertEquals(100.0, snapshot.getCollectorWindowSnapshot("G1-Young-Generation", "30sec").getPauseHistogramSnapshot().getMax(), 0.001);
        assertEquals(10.0, snapshot.getCollectorWindowSnapshot("G1-Young-Generation", "5min").getPercentageSpentInGc(), 0.001);
        assertEquals(100.0, snapshot.getCollectorWindowSnapshot("G1-Young-Generation", "5min").getPauseHistogramSnapshot().getMin(), 0.001);
        assertEquals(100.0, snapshot.getCollectorWindowSnapshot("G1-Young-Generation", "5min").getPauseHistogramSnapshot().getMax(), 0.001);
        assertEquals(10.0, snapshot.getCollectorWindowSnapshot("G1-Young-Generation", "uniform").getPercentageSpentInGc(), 0.001);
        assertEquals(100.0, snapshot.getCollectorWindowSnapshot("G1-Young-Generation", "uniform").getPauseHistogramSnapshot().getMin(), 0.001);
        assertEquals(100.0, snapshot.getCollectorWindowSnapshot("G1-Young-Generation", "uniform").getPauseHistogramSnapshot().getMax(), 0.001);
        assertEquals(0.0, snapshot.getCollectorWindowSnapshot("G1-Old-Generation", "30sec").getPercentageSpentInGc(), 0.001);
        assertEquals(0.0, snapshot.getCollectorWindowSnapshot("G1-Old-Generation", "30sec").getPauseHistogramSnapshot().getMin(), 0.001);
        assertEquals(0.0, snapshot.getCollectorWindowSnapshot("G1-Old-Generation", "30sec").getPauseHistogramSnapshot().getMax(), 0.001);
        assertEquals(0.0, snapshot.getCollectorWindowSnapshot("G1-Old-Generation", "5min").getPercentageSpentInGc(), 0.001);
        assertEquals(0.0, snapshot.getCollectorWindowSnapshot("G1-Old-Generation", "5min").getPauseHistogramSnapshot().getMin(), 0.001);
        assertEquals(0.0, snapshot.getCollectorWindowSnapshot("G1-Old-Generation", "5min").getPauseHistogramSnapshot().getMax(), 0.001);
        assertEquals(0.0, snapshot.getCollectorWindowSnapshot("G1-Old-Generation", "uniform").getPercentageSpentInGc(), 0.001);
        assertEquals(0.0, snapshot.getCollectorWindowSnapshot("G1-Old-Generation", "uniform").getPauseHistogramSnapshot().getMin(), 0.001);
        assertEquals(0.0, snapshot.getCollectorWindowSnapshot("G1-Old-Generation", "uniform").getPauseHistogramSnapshot().getMax(), 0.001);
        assertEquals(10.0, snapshot.getCollectorWindowSnapshot("AggregatedCollector", "30sec").getPercentageSpentInGc(), 0.001);
        assertEquals(100.0, snapshot.getCollectorWindowSnapshot("AggregatedCollector", "30sec").getPauseHistogramSnapshot().getMin(), 0.001);
        assertEquals(100.0, snapshot.getCollectorWindowSnapshot("AggregatedCollector", "30sec").getPauseHistogramSnapshot().getMax(), 0.001);
        assertEquals(10.0, snapshot.getCollectorWindowSnapshot("AggregatedCollector", "5min").getPercentageSpentInGc(), 0.001);
        assertEquals(100.0, snapshot.getCollectorWindowSnapshot("AggregatedCollector", "30sec").getPauseHistogramSnapshot().getMin(), 0.001);
        assertEquals(100.0, snapshot.getCollectorWindowSnapshot("AggregatedCollector", "30sec").getPauseHistogramSnapshot().getMax(), 0.001);
        assertEquals(10.0, snapshot.getCollectorWindowSnapshot("AggregatedCollector", "uniform").getPercentageSpentInGc(), 0.001);
        assertEquals(100.0, snapshot.getCollectorWindowSnapshot("AggregatedCollector", "30sec").getPauseHistogramSnapshot().getMin(), 0.001);
        assertEquals(100.0, snapshot.getCollectorWindowSnapshot("AggregatedCollector", "30sec").getPauseHistogramSnapshot().getMax(), 0.001);

        ticker.setCurrentTimeMillis(10_000);
        old.addFakeCollection(2000);
        snapshot = monitor.getSnapshot();
        assertEquals(1.0, snapshot.getCollectorWindowSnapshot("G1-Young-Generation", "30sec").getPercentageSpentInGc(), 0.001);
        assertEquals(1.0, snapshot.getCollectorWindowSnapshot("G1-Young-Generation", "5min").getPercentageSpentInGc(), 0.001);
        assertEquals(1.0, snapshot.getCollectorWindowSnapshot("G1-Young-Generation", "uniform").getPercentageSpentInGc(), 0.001);
        assertEquals(20.0, snapshot.getCollectorWindowSnapshot("G1-Old-Generation", "30sec").getPercentageSpentInGc(), 0.001);
        assertEquals(20.0, snapshot.getCollectorWindowSnapshot("G1-Old-Generation", "5min").getPercentageSpentInGc(), 0.001);
        assertEquals(20.0, snapshot.getCollectorWindowSnapshot("G1-Old-Generation", "uniform").getPercentageSpentInGc(), 0.001);
        assertEquals(21.0, snapshot.getCollectorWindowSnapshot("AggregatedCollector", "30sec").getPercentageSpentInGc(), 0.001);
        assertEquals(100.0, snapshot.getCollectorWindowSnapshot("AggregatedCollector", "30sec").getPauseHistogramSnapshot().getMin(), 0.001);
        assertEquals(2000.0, snapshot.getCollectorWindowSnapshot("AggregatedCollector", "30sec").getPauseHistogramSnapshot().getMax(), 0.001);
        assertEquals(21.0, snapshot.getCollectorWindowSnapshot("AggregatedCollector", "5min").getPercentageSpentInGc(), 0.001);
        assertEquals(100.0, snapshot.getCollectorWindowSnapshot("AggregatedCollector", "5min").getPauseHistogramSnapshot().getMin(), 0.001);
        assertEquals(2000.0, snapshot.getCollectorWindowSnapshot("AggregatedCollector", "5min").getPauseHistogramSnapshot().getMax(), 0.001);
        assertEquals(21.0, snapshot.getCollectorWindowSnapshot("AggregatedCollector", "uniform").getPercentageSpentInGc(), 0.001);
        assertEquals(100.0, snapshot.getCollectorWindowSnapshot("AggregatedCollector", "uniform").getPauseHistogramSnapshot().getMin(), 0.001);
        assertEquals(2000.0, snapshot.getCollectorWindowSnapshot("AggregatedCollector", "uniform").getPauseHistogramSnapshot().getMax(), 0.001);

        ticker.setCurrentTimeMillis(50_000);
        old.addFakeCollection(900);
        snapshot = monitor.getSnapshot();
        assertEquals(0.0, snapshot.getCollectorWindowSnapshot("G1-Young-Generation", "30sec").getPercentageSpentInGc(), 0.001);
        assertEquals(0.0, snapshot.getCollectorWindowSnapshot("G1-Young-Generation", "30sec").getPauseHistogramSnapshot().getMin(), 0.001);
        assertEquals(0.0, snapshot.getCollectorWindowSnapshot("G1-Young-Generation", "30sec").getPauseHistogramSnapshot().getMax(), 0.001);
        assertEquals(0.2, snapshot.getCollectorWindowSnapshot("G1-Young-Generation", "5min").getPercentageSpentInGc(), 0.001);
        assertEquals(0.0, snapshot.getCollectorWindowSnapshot("G1-Young-Generation", "30sec").getPauseHistogramSnapshot().getMin(), 0.001);
        assertEquals(0.0, snapshot.getCollectorWindowSnapshot("G1-Young-Generation", "30sec").getPauseHistogramSnapshot().getMax(), 0.001);
        assertEquals(0.2, snapshot.getCollectorWindowSnapshot("G1-Young-Generation", "uniform").getPercentageSpentInGc(), 0.001);
        assertEquals(100.0, snapshot.getCollectorWindowSnapshot("G1-Young-Generation", "uniform").getPauseHistogramSnapshot().getMin(), 0.001);
        assertEquals(100.0, snapshot.getCollectorWindowSnapshot("G1-Young-Generation", "uniform").getPauseHistogramSnapshot().getMax(), 0.001);
        assertEquals(3.0, snapshot.getCollectorWindowSnapshot("G1-Old-Generation", "30sec").getPercentageSpentInGc(), 0.001);
        assertEquals(900.0, snapshot.getCollectorWindowSnapshot("G1-Old-Generation", "30sec").getPauseHistogramSnapshot().getMin(), 0.001);
        assertEquals(900.0, snapshot.getCollectorWindowSnapshot("G1-Old-Generation", "30sec").getPauseHistogramSnapshot().getMax(), 0.001);
        assertEquals(5.8, snapshot.getCollectorWindowSnapshot("G1-Old-Generation", "5min").getPercentageSpentInGc(), 0.001);
        assertEquals(900.0, snapshot.getCollectorWindowSnapshot("G1-Old-Generation", "5min").getPauseHistogramSnapshot().getMin(), 0.001);
        assertEquals(2000.0, snapshot.getCollectorWindowSnapshot("G1-Old-Generation", "5min").getPauseHistogramSnapshot().getMax(), 0.001);
        assertEquals(5.8, snapshot.getCollectorWindowSnapshot("G1-Old-Generation", "uniform").getPercentageSpentInGc(), 0.001);
        assertEquals(900.0, snapshot.getCollectorWindowSnapshot("G1-Old-Generation", "uniform").getPauseHistogramSnapshot().getMin(), 0.001);
        assertEquals(2000.0, snapshot.getCollectorWindowSnapshot("G1-Old-Generation", "uniform").getPauseHistogramSnapshot().getMax(), 0.001);
        assertEquals(3.0, snapshot.getCollectorWindowSnapshot("AggregatedCollector", "30sec").getPercentageSpentInGc(), 0.001);
        assertEquals(900.0, snapshot.getCollectorWindowSnapshot("AggregatedCollector", "30sec").getPauseHistogramSnapshot().getMin(), 0.001);
        assertEquals(900.0, snapshot.getCollectorWindowSnapshot("AggregatedCollector", "30sec").getPauseHistogramSnapshot().getMax(), 0.001);
        assertEquals(6.0, snapshot.getCollectorWindowSnapshot("AggregatedCollector", "5min").getPercentageSpentInGc(), 0.001);
        assertEquals(100.0, snapshot.getCollectorWindowSnapshot("AggregatedCollector", "5min").getPauseHistogramSnapshot().getMin(), 0.001);
        assertEquals(2000.0, snapshot.getCollectorWindowSnapshot("AggregatedCollector", "5min").getPauseHistogramSnapshot().getMax(), 0.001);
        assertEquals(6.0, snapshot.getCollectorWindowSnapshot("AggregatedCollector", "uniform").getPercentageSpentInGc(), 0.001);
        assertEquals(100.0, snapshot.getCollectorWindowSnapshot("AggregatedCollector", "uniform").getPauseHistogramSnapshot().getMin(), 0.001);
        assertEquals(2000.0, snapshot.getCollectorWindowSnapshot("AggregatedCollector", "uniform").getPauseHistogramSnapshot().getMax(), 0.001);

        ticker.setCurrentTimeMillis(600_000);
        snapshot = monitor.getSnapshot();
        assertEquals(0.0, snapshot.getCollectorWindowSnapshot("G1-Young-Generation", "30sec").getPercentageSpentInGc(), 0.001);
        assertEquals(0.0, snapshot.getCollectorWindowSnapshot("G1-Young-Generation", "30sec").getPauseHistogramSnapshot().getMin(), 0.001);
        assertEquals(0.0, snapshot.getCollectorWindowSnapshot("G1-Young-Generation", "30sec").getPauseHistogramSnapshot().getMax(), 0.001);;
        assertEquals(0.0, snapshot.getCollectorWindowSnapshot("G1-Young-Generation", "5min").getPercentageSpentInGc(), 0.001);
        assertEquals(0.0, snapshot.getCollectorWindowSnapshot("G1-Young-Generation", "5min").getPauseHistogramSnapshot().getMin(), 0.001);
        assertEquals(0.0, snapshot.getCollectorWindowSnapshot("G1-Young-Generation", "5min").getPauseHistogramSnapshot().getMax(), 0.001);
        assertEquals(0.016, snapshot.getCollectorWindowSnapshot("G1-Young-Generation", "uniform").getPercentageSpentInGc(), 0.001);
        assertEquals(100.0, snapshot.getCollectorWindowSnapshot("G1-Young-Generation", "uniform").getPauseHistogramSnapshot().getMin(), 0.001);
        assertEquals(100.0, snapshot.getCollectorWindowSnapshot("G1-Young-Generation", "uniform").getPauseHistogramSnapshot().getMax(), 0.001);
        assertEquals(0.0, snapshot.getCollectorWindowSnapshot("G1-Old-Generation", "30sec").getPercentageSpentInGc(), 0.001);
        assertEquals(0.0, snapshot.getCollectorWindowSnapshot("G1-Old-Generation", "30sec").getPauseHistogramSnapshot().getMin(), 0.001);
        assertEquals(0.0, snapshot.getCollectorWindowSnapshot("G1-Old-Generation", "30sec").getPauseHistogramSnapshot().getMax(), 0.001);
        assertEquals(0.0, snapshot.getCollectorWindowSnapshot("G1-Old-Generation", "5min").getPercentageSpentInGc(), 0.001);
        assertEquals(0.0, snapshot.getCollectorWindowSnapshot("G1-Old-Generation", "5min").getPauseHistogramSnapshot().getMin(), 0.001);
        assertEquals(0.0, snapshot.getCollectorWindowSnapshot("G1-Old-Generation", "5min").getPauseHistogramSnapshot().getMax(), 0.001);
        assertEquals(0.483, snapshot.getCollectorWindowSnapshot("G1-Old-Generation", "uniform").getPercentageSpentInGc(), 0.001);
        assertEquals(900.0, snapshot.getCollectorWindowSnapshot("G1-Old-Generation", "uniform").getPauseHistogramSnapshot().getMin(), 0.001);
        assertEquals(2000.0, snapshot.getCollectorWindowSnapshot("G1-Old-Generation", "uniform").getPauseHistogramSnapshot().getMax(), 0.001);
        assertEquals(0.0, snapshot.getCollectorWindowSnapshot("AggregatedCollector", "30sec").getPercentageSpentInGc(), 0.001);
        assertEquals(0.0, snapshot.getCollectorWindowSnapshot("AggregatedCollector", "30sec").getPauseHistogramSnapshot().getMin(), 0.001);
        assertEquals(0.0, snapshot.getCollectorWindowSnapshot("AggregatedCollector", "30sec").getPauseHistogramSnapshot().getMax(), 0.001);
        assertEquals(0.0, snapshot.getCollectorWindowSnapshot("AggregatedCollector", "5min").getPercentageSpentInGc(), 0.001);
        assertEquals(0.0, snapshot.getCollectorWindowSnapshot("AggregatedCollector", "5min").getPauseHistogramSnapshot().getMin(), 0.001);
        assertEquals(0.0, snapshot.getCollectorWindowSnapshot("AggregatedCollector", "5min").getPauseHistogramSnapshot().getMax(), 0.001);
        assertEquals(0.5, snapshot.getCollectorWindowSnapshot("AggregatedCollector", "uniform").getPercentageSpentInGc(), 0.001);
        assertEquals(100.0, snapshot.getCollectorWindowSnapshot("AggregatedCollector", "uniform").getPauseHistogramSnapshot().getMin(), 0.001);
        assertEquals(2000.0, snapshot.getCollectorWindowSnapshot("AggregatedCollector", "uniform").getPauseHistogramSnapshot().getMax(), 0.001);

    }

}
