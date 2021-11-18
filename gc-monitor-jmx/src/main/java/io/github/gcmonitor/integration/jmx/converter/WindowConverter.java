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

package io.github.gcmonitor.integration.jmx.converter;

import io.github.gcmonitor.GcMonitorConfiguration;
import io.github.gcmonitor.stat.GcMonitorSnapshot;

import javax.management.openmbean.*;

public class WindowConverter implements Converter {

    public static final String TYPE_DESCRIPTION = "Shows aggregated information about particular garbage collector window";
    public static final String TYPE_NAME = CollectorConverter.TYPE_NAME + ".window";

    private static final String UTILIZATION_FIELD = "utilization";
    private static final String PAUSE_HISTOGRAM_FIELD = "pauseHistogram";
    private static final String[] itemNames = new String[] {
            UTILIZATION_FIELD,
            PAUSE_HISTOGRAM_FIELD
    };

    private final CompositeType type;
    private final UtilizationConverter utilizationConverter;
    private final LatencyHistogramConverter latencyHistogramConverter;

    public WindowConverter(GcMonitorConfiguration configuration, String collectorName, String windowName) throws OpenDataException {
        this.utilizationConverter = new UtilizationConverter(configuration, collectorName, windowName);
        this.latencyHistogramConverter = new LatencyHistogramConverter(configuration, collectorName, windowName);

        String[] itemDescriptions = new String[] {
                "GC latency histogram",
                "Collector utilization"
        };
        OpenType<?>[] itemTypes = new OpenType<?>[] {
                utilizationConverter.getType(),
                latencyHistogramConverter.getType()
        };
        this.type = new CompositeType(TYPE_NAME, TYPE_DESCRIPTION, itemNames, itemDescriptions, itemTypes);
    }

    @Override
    public CompositeData map(GcMonitorSnapshot snapshot) {
        Object[] itemValues = {
                utilizationConverter.map(snapshot),
                latencyHistogramConverter.map(snapshot)
        };
        try {
            return new CompositeDataSupport(type, itemNames, itemValues);
        } catch (OpenDataException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public CompositeType getType() {
        return type;
    }

}
