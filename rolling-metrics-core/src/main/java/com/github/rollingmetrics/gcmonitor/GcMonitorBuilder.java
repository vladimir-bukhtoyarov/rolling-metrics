
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
import java.time.Duration;
import java.util.*;


public class GcMonitorBuilder {

    private final SortedMap<String, WindowSpecification> windows;
    private final List<GarbageCollectorMXBean> garbageCollectorMXBeans;
    private double[] percentiles = GcMonitorConfiguration.DEFAULT_PERCENTILES;
    private boolean aggregateStatFromDifferentCollectors = true;
    private int histogramSignificantDigits = GcMonitorConfiguration.DEFAULT_HISTOGRAM_SIGNIFICANT_DIGITS;
    private Ticker ticker = Ticker.defaultTicker();

    public GcMonitor build() {
        return new GcMonitor(createConfiguration());
    }

    GcMonitorBuilder(Collection<GarbageCollectorMXBean> garbageCollectorMXBeans) {
        checkMbeans(garbageCollectorMXBeans);
        this.windows = new TreeMap<>();
        this.windows.put(GcMonitorConfiguration.UNIFORM_WINDOW_NAME, WindowSpecification.uniform());
        this.garbageCollectorMXBeans = new ArrayList<>(garbageCollectorMXBeans);
    }

    public GcMonitorBuilder withPercentiles(double[] percentiles) {
        validatePerventiles(percentiles);
        this.percentiles = percentiles.clone();
        Arrays.sort(this.percentiles);
        return this;
    }

    public GcMonitorBuilder addRollingWindow(String windowName, Duration rollingWindow) {
        validateRollingWindow(windowName, rollingWindow);
        windows.put(windowName, WindowSpecification.rollingWindow(rollingWindow, ticker));
        return this;
    }

    public GcMonitorBuilder withoutUniformWindow() {
        windows.remove(GcMonitorConfiguration.UNIFORM_WINDOW_NAME);
        return this;
    }

    public GcMonitorBuilder withTicker(Ticker ticker) {
        if (ticker == null) {
            throw new IllegalArgumentException("ticker should not be null");
        }
        this.ticker = ticker;
        return this;
    }

    public GcMonitorBuilder withoutCollectorsAggregation() {
        this.aggregateStatFromDifferentCollectors = false;
        return this;
    }

    public GcMonitorBuilder withHistogramSignificantDigits(int histogramSignificantDigits) {
        this.histogramSignificantDigits = histogramSignificantDigits;
        return this;
    }

    GcMonitorConfiguration createConfiguration() {
        return new GcMonitorConfiguration(windows, percentiles, histogramSignificantDigits, garbageCollectorMXBeans, aggregateStatFromDifferentCollectors, ticker);
    }

    private void checkMbeans(Collection<GarbageCollectorMXBean> garbageCollectorMXBeans) {
        if (garbageCollectorMXBeans == null) {
            throw new IllegalArgumentException("garbageCollectorMXBeans should not be null");
        }
        if (garbageCollectorMXBeans.isEmpty()) {
            throw new IllegalArgumentException("garbageCollectorMXBeans should not be empty");
        }
        Set<String> names = new HashSet<>();
        for (GarbageCollectorMXBean bean : garbageCollectorMXBeans) {
            if (bean == null) {
                throw new IllegalArgumentException("garbageCollectorMXBeans should not contain null elements");
            }
            String name = NamingUtils.replaceAllWhitespaces(bean.getName());
            if (names.contains(name)) {
                throw new IllegalArgumentException("garbageCollectorMXBeans contains two collectors with name [" + name + "]");
            }
            names.add(name);
        }
    }

    private void validatePerventiles(double[] percentiles) {
        if (percentiles == null) {
            throw new IllegalArgumentException("percentiles array should not be null");
        }
        if (percentiles.length == 0) {
            throw new IllegalArgumentException("percentiles array should not be empty");
        }

        for (int i = 0; i < percentiles.length; i++) {
            if (percentiles[i] <= 0.0 || percentiles[i] > 1.0) {
                String msg = "Wrong percentile " + percentiles[i] + " at index " + i + ", percentile should be between 0 and 1";
                throw new IllegalArgumentException(msg);
            }
            for (int j = i + 1; j < percentiles.length; j++) {
                if (percentiles[i] == percentiles[j]) {
                    String msg = "percentile " + percentiles[i] + " has been duplicated at positions [" + i + "," + j + "]";
                    throw new IllegalArgumentException(msg);
                }
            }
        }
    }

    private void validateRollingWindow(String windowName, Duration rollingWindow) {
        if (windowName == null) {
            throw new IllegalArgumentException("windowName should not be null");
        }
        if (windows.containsKey(windowName)) {
            throw new IllegalArgumentException("Window with name " + windowName + " already configured");
        }
        if (rollingWindow == null) {
            throw new IllegalArgumentException("rollingWindow should not be null");
        }
        if (rollingWindow.isZero() || rollingWindow.isNegative()) {
            throw new IllegalArgumentException("rollingWindow should be a positive duration");
        }
    }

}