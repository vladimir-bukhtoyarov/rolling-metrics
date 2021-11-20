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

package com.github.rollingmetrics.gcmonitor;


import com.github.rollingmetrics.util.NamingUtils;
import com.github.rollingmetrics.util.Ticker;
import com.github.rollingmetrics.gcmonitor.stat.WindowSpecification;

import java.lang.management.GarbageCollectorMXBean;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class GcMonitorConfiguration {

    public static final double[] DEFAULT_PERCENTILES = new double[] {0.5, 0.75, 0.9, 0.95, 0.98, 0.99, 0.999};
    public static final String AGGREGATED_COLLECTOR_NAME = "AggregatedCollector";
    public static final String UNIFORM_WINDOW_NAME = "uniform";
    public static final int COUNTER_CHUNKS = 10;
    public static final int HISTOGRAM_CHUNKS = 5;
    public static final long LONGEST_TRACKABLE_PAUSE_MILLIS = TimeUnit.DAYS.toMillis(1);
    public static int DECIMAL_POINTS = 3;
    public static int DEFAULT_HISTOGRAM_SIGNIFICANT_DIGITS = 2;

    private final List<GarbageCollectorMXBean> garbageCollectorMXBeans;
    private final SortedSet<String> collectorNames;
    private final SortedMap<String, WindowSpecification> windowSpecifications;
    private final double[] percentiles;
    private final int histogramSignificantDigits;
    private final boolean aggregateDifferentCollectors;
    private final Ticker ticker;

    GcMonitorConfiguration(SortedMap<String, WindowSpecification> windowSpecifications, double[] percentiles, int histogramSignificantDigits, List<GarbageCollectorMXBean> garbageCollectorMXBeans, boolean aggregateDifferentCollectors, Ticker ticker) {
        this.windowSpecifications = Collections.unmodifiableSortedMap(windowSpecifications);
        this.garbageCollectorMXBeans = Collections.unmodifiableList(garbageCollectorMXBeans);
        this.percentiles = percentiles.clone();
        this.histogramSignificantDigits = histogramSignificantDigits;
        this.aggregateDifferentCollectors = aggregateDifferentCollectors;
        this.ticker = ticker;

        this.collectorNames = new TreeSet<>();
        garbageCollectorMXBeans.forEach(bean -> collectorNames.add(NamingUtils.replaceAllWhitespaces(bean.getName())));
        if (aggregateDifferentCollectors && garbageCollectorMXBeans.size() > 1) {
            collectorNames.add(AGGREGATED_COLLECTOR_NAME);
        }
    }

    public List<GarbageCollectorMXBean> getGarbageCollectorMXBeans() {
        return garbageCollectorMXBeans;
    }

    public SortedMap<String, WindowSpecification> getWindowSpecifications() {
        return windowSpecifications;
    }

    public Set<String> getWindowNames() {
        return windowSpecifications.keySet();
    }

    public SortedSet<String> getCollectorNames() {
        return collectorNames;
    }

    public Ticker getTicker() {
        return ticker;
    }

    public double[] getPercentiles() {
        return percentiles;
    }

    public int getCounterChunks() {
        return COUNTER_CHUNKS;
    }

    public int getHistogramChunks() {
        return HISTOGRAM_CHUNKS;
    }

    public int getHistogramSignificantDigits() {
        return histogramSignificantDigits;
    }

    public long getLongestTrackablePauseMillis() {
        return LONGEST_TRACKABLE_PAUSE_MILLIS;
    }

    public int getDecimalPoints() {
        return DECIMAL_POINTS = 2;
    }

    public boolean isAggregateDifferentCollectors() {
        return aggregateDifferentCollectors;
    }
}
