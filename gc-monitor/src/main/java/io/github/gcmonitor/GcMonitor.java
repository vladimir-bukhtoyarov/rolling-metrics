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

import com.github.rollingmetrics.histogram.hdr.RollingHdrHistogram;
import com.github.rollingmetrics.util.NamingUtils;
import io.github.gcmonitor.stat.GcMonitorSnapshot;
import io.github.gcmonitor.stat.GcStatistics;

import javax.management.ListenerNotFoundException;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.Collection;

public class GcMonitor implements NotificationListener {

    private final GcMonitorConfiguration configuration;

    private final GcStatistics statistics;

    private boolean started = false;
    private boolean stopped = false;

    public static GcMonitorBuilder builder() {
        Collection<GarbageCollectorMXBean> garbageCollectorMXBeans = ManagementFactory.getGarbageCollectorMXBeans();
        return new GcMonitorBuilder(garbageCollectorMXBeans);
    }

    public static GcMonitorBuilder builder(Collection<GarbageCollectorMXBean> garbageCollectorMXBeans) {
        return new GcMonitorBuilder(garbageCollectorMXBeans);
    }

    GcMonitor(GcMonitorConfiguration configuration) {
        this.configuration = configuration;
        this.statistics = GcStatistics.create(configuration);
    }

    synchronized public GcMonitorSnapshot getSnapshot() {
        return statistics.getSnapshot();
    }

    synchronized public long getMillisSpentInGc(String collectorName, String windowName) {
        return statistics.getMillisSpentInGc(collectorName, windowName);
    }

    synchronized public double getPausePercentage(String collectorName, String windowName) {
        return statistics.getPausePercentage(collectorName, windowName);
    }

    synchronized public long getTotalPauseCount(String collectorName, String windowName) {
        return statistics.getTotalPauseCount(collectorName, windowName);
    }

    public RollingHdrHistogram getPauseLatencyHistogram(String collectorName, String windowName) {
        return statistics.getCollectorLatencyHistogram(collectorName, windowName);
    }

    @Override
    synchronized public void handleNotification(Notification notification, Object handback) {
        String collectorName = (String) handback;
        statistics.handleNotification(collectorName, notification);
    }

    public synchronized GcMonitorConfiguration getConfiguration() {
        return configuration;
    }

    public synchronized void start() {
        if (started) {
            // already started
            return;
        }
        started = true;

        for (GarbageCollectorMXBean bean : configuration.getGarbageCollectorMXBeans()) {
            NotificationEmitter emitter = (NotificationEmitter) bean;
            emitter.addNotificationListener(this, null, NamingUtils.replaceAllWhitespaces(bean.getName()));
        }
    }

    public synchronized void stop() {
        if (stopped || !started) {
            return;
        }
        stopped = true;

        for (GarbageCollectorMXBean bean : configuration.getGarbageCollectorMXBeans()) {
            try {
                ((NotificationEmitter) bean).removeNotificationListener(this);
            } catch (ListenerNotFoundException e) {
                // Do nothing
            }
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("GcMonitor{");
        sb.append("configuration=").append(configuration);
        sb.append(", snapshot=").append(getSnapshot());
        sb.append(", started=").append(started);
        sb.append(", stopped=").append(stopped);
        sb.append('}');
        return sb.toString();
    }

}
