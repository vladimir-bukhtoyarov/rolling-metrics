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

package com.github.rollingmetrics.micrometer.gcmonitor;

import com.github.rollingmetrics.histogram.hdr.RollingHdrHistogram;
import com.github.rollingmetrics.gcmonitor.GcMonitor;
import com.github.rollingmetrics.gcmonitor.GcMonitorConfiguration;
import com.github.rollingmetrics.gcmonitor.stat.Formatter;
import com.github.rollingmetrics.histogram.hdr.RollingSnapshot;
import com.github.rollingmetrics.micrometer.meters.RollingDistributionSummary;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.distribution.HistogramGauges;


/**
 * TODO add javadocs
 */
public class GcMonitorMeterBinder implements MeterBinder {
    // https://micrometer.io/docs/concepts#_why_is_my_gauge_reporting_nan_or_disappearing
    // We need maintain strong reference to state object and do not want to relly on gc monitor reachability.
    private static final Object stateObject = new Object();

    private final String namePrefix;
    private final GcMonitor monitor;

    public GcMonitorMeterBinder(String namePrefix, GcMonitor monitor){
        this.namePrefix = namePrefix;
        this.monitor = monitor;
    }


    @Override
    public void bindTo(MeterRegistry meterRegistry) {
        GcMonitorConfiguration configuration = monitor.getConfiguration();
        for (String collectorName : configuration.getCollectorNames()) {
            for (String windowName : configuration.getWindowNames()) {
                addHistogram(namePrefix, meterRegistry, collectorName, windowName, monitor);
                addPercentageGauge(namePrefix, meterRegistry, collectorName, windowName, monitor);
                addGcDurationGauge(namePrefix, meterRegistry, collectorName, windowName, monitor);
                addTotalPauseCountGauge(namePrefix, meterRegistry, collectorName, windowName, monitor);
            }
        }
    }

    private static void addHistogram(String namePrefix, MeterRegistry meterRegistry, String collectorName, String windowName, GcMonitor monitor) {
        RollingHdrHistogram histogram = monitor.getPauseLatencyHistogram(collectorName, windowName);
        SnapshotCache snapshotCache = new SnapshotCache(histogram);
        Tags tags = tags(collectorName, windowName);

        for (double percentile : monitor.getConfiguration().getPercentiles()){
            meterRegistry.gauge(namePrefix+".pauseLatencyMillis"+".p"+percentile, tags,
                    stateObject,
                    ignore -> snapshotCache.getSnapshot().getValue(percentile));
        }

        meterRegistry.gauge(namePrefix+".pauseLatencyMillis"+".median", tags,
                stateObject,
                ignore -> snapshotCache.getSnapshot().getMedian());
        meterRegistry.gauge(namePrefix+".pauseLatencyMillis"+".max", tags,
                stateObject,
                ignore -> snapshotCache.getSnapshot().getMax());
        meterRegistry.gauge(namePrefix+".pauseLatencyMillis"+".mean", tags,
                stateObject,
                ignore -> snapshotCache.getSnapshot().getMean());
        meterRegistry.gauge(namePrefix+".pauseLatencyMillis"+".min", tags,
                stateObject,
                ignore -> snapshotCache.getSnapshot().getMin());
        meterRegistry.gauge(namePrefix+".pauseLatencyMillis"+".stdDev", tags,
                stateObject,
                ignore -> snapshotCache.getSnapshot().getStdDev());
    }

    private static void addGcDurationGauge(String namePrefix, MeterRegistry meterRegistry, String collectorName, String windowName, GcMonitor monitor) {
        String name = namePrefix + "." + collectorName + "." + windowName + "." + "millisSpentInPause";
        meterRegistry.gauge(name, stateObject, m -> monitor.getMillisSpentInGc(collectorName, windowName));
    }

    private static void addPercentageGauge(String namePrefix, MeterRegistry meterRegistry, String collectorName, String windowName, GcMonitor monitor) {
        String name = namePrefix + "." + collectorName + "." + windowName + "." + "pausePercentage";
        meterRegistry.gauge(name, stateObject, m -> {
            double pausePercentage = monitor.getPausePercentage(collectorName, windowName);
            return Formatter.roundToDigits(pausePercentage, monitor.getConfiguration().getDecimalPoints()).doubleValue();
        });
    }

    private static void addTotalPauseCountGauge(String namePrefix, MeterRegistry meterRegistry, String collectorName, String windowName, GcMonitor monitor) {
        String name = namePrefix + ".pauseCount";
        meterRegistry.gauge(name, tags(collectorName, windowName), stateObject, m -> monitor.getTotalPauseCount(collectorName, windowName));
    }

    private static Tags tags(String collectorName, String windowName){
        return Tags.of("collectorName", collectorName, "windowName", windowName);
    }

    /**
     * Cache to preserve snapshot between evaluating distinct gauges within single collection
     */
    private static class SnapshotCache {
        private final RollingHdrHistogram histogram;
        private volatile RollingSnapshot snapshot;
        private volatile long lastCalculation;
        private final long cachePeriod = 1_000;

        public SnapshotCache(RollingHdrHistogram histogram) {
            this.histogram = histogram;
        }

        public RollingSnapshot getSnapshot() {
            if (Math.abs(System.currentTimeMillis() - lastCalculation) > cachePeriod) {
                return lockAndCalculateNewSnapshot();
            } else {
                return snapshot;
            }
        }

        private synchronized RollingSnapshot lockAndCalculateNewSnapshot() {
            if (Math.abs(System.currentTimeMillis() - lastCalculation) > cachePeriod) {
                snapshot = histogram.getSnapshot();
                lastCalculation = System.currentTimeMillis();
            }
            return snapshot;
        }
    }

}
