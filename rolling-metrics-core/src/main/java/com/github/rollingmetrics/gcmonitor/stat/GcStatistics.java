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

package com.github.rollingmetrics.gcmonitor.stat;

import com.github.rollingmetrics.histogram.hdr.RollingHdrHistogram;
import com.github.rollingmetrics.util.NamingUtils;
import com.github.rollingmetrics.gcmonitor.GcMonitorConfiguration;

import javax.management.Notification;
import java.lang.management.GarbageCollectorMXBean;
import java.util.*;

public class GcStatistics {

    private final GcMonitorConfiguration configuration;
    private final SortedMap<String, MonitoredCollector> monitoredCollectors;
    private final SortedMap<String, CollectorStatistics> perCollectorStatistics;

    public static GcStatistics create(GcMonitorConfiguration configuration) {
        SortedMap<String, MonitoredCollector> monitoredCollectors = new TreeMap<>();
        SortedMap<String, CollectorStatistics> perCollectorStatistics = new TreeMap<>();
        List<GarbageCollectorMXBean> garbageCollectorMXBeans = configuration.getGarbageCollectorMXBeans();
        long currentTimeMillis = configuration.getTicker().stableMilliseconds();

        Optional<CollectorStatistics> aggregatedStatistics;
        if (garbageCollectorMXBeans.size() > 1 && configuration.isAggregateDifferentCollectors()) {
            aggregatedStatistics = Optional.of(createCollectorStatistics(configuration, currentTimeMillis));
            perCollectorStatistics.put(GcMonitorConfiguration.AGGREGATED_COLLECTOR_NAME, aggregatedStatistics.get());
        } else {
            aggregatedStatistics = Optional.empty();
        }

        for (GarbageCollectorMXBean bean : configuration.getGarbageCollectorMXBeans()) {
            CollectorStatistics collectorStatistics = createCollectorStatistics(configuration, currentTimeMillis);
            String collectorName = NamingUtils.replaceAllWhitespaces(bean.getName());
            perCollectorStatistics.put(collectorName, collectorStatistics);
            MonitoredCollector monitoredCollector = new MonitoredCollector(bean, aggregatedStatistics, collectorStatistics);
            monitoredCollectors.put(collectorName, monitoredCollector);
        }
        return new GcStatistics(configuration, monitoredCollectors, perCollectorStatistics);
    }

    private static CollectorStatistics createCollectorStatistics(GcMonitorConfiguration configuration, long currentTimeMillis) {
        SortedMap<String, CollectorWindow> windows = new TreeMap<>();
        configuration.getWindowSpecifications().forEach((windowName, windowSpec) -> {
            CollectorWindow window = windowSpec.createWindow(currentTimeMillis, configuration);
            windows.put(windowName, window);
        });
        return new CollectorStatistics(windows);
    }

    private GcStatistics(GcMonitorConfiguration configuration, SortedMap<String, MonitoredCollector> monitoredCollectors, SortedMap<String, CollectorStatistics> perCollectorStatistics) {
        this.configuration = configuration;
        this.monitoredCollectors = monitoredCollectors;
        this.perCollectorStatistics = perCollectorStatistics;
    }

    public void handleNotification(String collectorName, Notification notification) {
        MonitoredCollector monitoredCollector = monitoredCollectors.get(collectorName);
        monitoredCollector.handleNotification(notification);
    }

    public GcMonitorSnapshot getSnapshot() {
        long currentTimeMillis = configuration.getTicker().stableMilliseconds();

        Map<String, Map<String, CollectorWindowSnapshot>> perCollectorSnapshots = new HashMap<>();
        for (Map.Entry<String, CollectorStatistics> collectorEntry: perCollectorStatistics.entrySet()) {
            String collectorName = collectorEntry.getKey();
            CollectorStatistics collectorStatistics = collectorEntry.getValue();
            Map<String, CollectorWindowSnapshot> collectorMap = new HashMap<>();
            perCollectorSnapshots.put(collectorName, collectorMap);

            for (Map.Entry<String, CollectorWindow> windowEntry : collectorStatistics.getWindows().entrySet()) {
                String windowName = windowEntry.getKey();
                CollectorWindow window = windowEntry.getValue();
                CollectorWindowSnapshot windowSnapshot = window.getSnapshot(currentTimeMillis);
                collectorMap.put(windowName, windowSnapshot);
            }
        }
        return new GcMonitorSnapshot(perCollectorSnapshots);
    }

    public CollectorWindowSnapshot getCollectorWindowSnapshot(String collectorName, String windowName) {
        CollectorWindow window = getCollectorWindow(collectorName, windowName);
        return window.getSnapshot(configuration.getTicker().stableMilliseconds());
    }

    public RollingHdrHistogram getCollectorLatencyHistogram(String collectorName, String windowName) {
        CollectorWindow window = getCollectorWindow(collectorName, windowName);
        return window.getReadOnlyPauseLatencyHistogram();
    }

    public long getMillisSpentInGc(String collectorName, String windowName) {
        CollectorWindow window = getCollectorWindow(collectorName, windowName);
        return window.getMillisSpentInGc(configuration.getTicker().stableMilliseconds());
    }

    public long getTotalPauseCount(String collectorName, String windowName) {
        return getCollectorWindow(collectorName, windowName).getPauseCount();
    }

    public double getPausePercentage(String collectorName, String windowName) {
        CollectorWindow window = getCollectorWindow(collectorName, windowName);
        return window.getPausePercentage(configuration.getTicker().stableMilliseconds());
    }

    public CollectorWindow getCollectorWindow(String collectorName, String windowName) {
        CollectorStatistics collectorWindows = getCollectorStatistics(collectorName);

        CollectorWindow window = collectorWindows.getWindows().get(windowName);
        if (window == null) {
            throw new IllegalArgumentException("Unknown name of collector window [" + windowName + "]");
        }
        return window;
    }

    private CollectorStatistics getCollectorStatistics(String collectorName) {
        CollectorStatistics collectorWindows = perCollectorStatistics.get(collectorName);
        if (collectorWindows == null) {
            throw new IllegalArgumentException("Unknown collector name [" + collectorName + "]");
        }
        return collectorWindows;
    }

}
