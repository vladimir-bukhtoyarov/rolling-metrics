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

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import com.github.rollingmetrics.dropwizard.Dropwizard;
import com.github.rollingmetrics.histogram.hdr.RollingHdrHistogram;
import com.github.rollingmetrics.gcmonitor.GcMonitor;
import com.github.rollingmetrics.gcmonitor.GcMonitorConfiguration;
import com.github.rollingmetrics.gcmonitor.stat.Formatter;

import java.math.BigDecimal;
import java.util.Map;
import java.util.TreeMap;



/**
 * Factory for Dropwizard Metrics compatible metric set.
 */
public class GcMonitorMetricSet {

    /**
     * Creates Dropwizard Metrics compatible collections of metric for gcMonitor
     *
     * @param namePrefix the prefix that will be added for each metric name
     * @param gcMonitor garbage collector monitor
     *
     * @return the Dropwizard Metrics compatible collections of metric for gcMonitor
     */
    public static MetricSet toMetricSet(String namePrefix, GcMonitor gcMonitor) {
        Map<String, Metric> metrics = new TreeMap<>();
        GcMonitorConfiguration configuration = gcMonitor.getConfiguration();
        for (String collectorName : configuration.getCollectorNames()) {
            for (String windowName : configuration.getWindowNames()) {
                addHistogram(namePrefix, metrics, collectorName, windowName, gcMonitor);
                addPercentageGauge(namePrefix, metrics, collectorName, windowName, gcMonitor);
                addGcDurationGauge(namePrefix, metrics, collectorName, windowName, gcMonitor);
            }
        }
        return () -> metrics;
    }

    private static void addHistogram(String namePrefix, Map<String, Metric> metrics, String collectorName, String windowName, GcMonitor monitor) {
        RollingHdrHistogram histogram = monitor.getPauseLatencyHistogram(collectorName, windowName);

        String name = namePrefix + "." + collectorName + "." + windowName + "." + "pauseLatencyMillis";
        Histogram dropwizardHistogram = new Histogram(Dropwizard.toReservoir(histogram)) {
            @Override
            public long getCount() {
                return monitor.getTotalPauseCount(collectorName, windowName);
            }
        };
        metrics.put(name, dropwizardHistogram);
    }

    private static void addGcDurationGauge(String namePrefix, Map<String, Metric> metrics, String collectorName, String windowName, GcMonitor monitor) {
        Gauge<Long> durationGauge = () -> monitor.getMillisSpentInGc(collectorName, windowName);
        String name = namePrefix + "." + collectorName + "." + windowName + "." + "millisSpentInPause";
        metrics.put(name, durationGauge);
    }

    private static void addPercentageGauge(String namePrefix, Map<String, Metric> metrics, String collectorName, String windowName, GcMonitor monitor) {
        Gauge<BigDecimal> durationGauge = () -> {
            double pausePercentage = monitor.getPausePercentage(collectorName, windowName);
            return Formatter.roundToDigits(pausePercentage, monitor.getConfiguration().getDecimalPoints());
        };
        String name = namePrefix + "." + collectorName + "." + windowName + "." + "pausePercentage";
        metrics.put(name, durationGauge);
    }

}
