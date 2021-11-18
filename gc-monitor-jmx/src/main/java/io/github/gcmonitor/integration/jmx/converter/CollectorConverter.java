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
import java.util.HashMap;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class CollectorConverter implements Converter {

    static final String TYPE_DESCRIPTION = "Shows aggregated information about particular garbage collector";
    static final String TYPE_NAME = SnapshotConverter.TYPE_NAME + ".collector";

    private final CompositeType type;
    private final SortedMap<String, WindowConverter> windowConverters = new TreeMap<>();

    CollectorConverter(GcMonitorConfiguration configuration, String collectorName) throws OpenDataException {
        Set<String> windowNames = configuration.getWindowNames();
        String[] itemNames = new String[windowNames.size()];
        String[] itemDescriptions = new String[windowNames.size()];
        OpenType<?>[] itemTypes = new OpenType<?>[windowNames.size()];

        int i = 0;
        for (String windowName : windowNames) {
            WindowConverter windowConverter = new WindowConverter(configuration, collectorName, windowName);
            windowConverters.put(windowName, windowConverter);
            itemNames[i] = windowName;
            itemDescriptions[i] = windowName;
            itemTypes[i] = windowConverter.getType();
            i++;
        }

        this.type = new CompositeType(TYPE_NAME, TYPE_DESCRIPTION, itemNames, itemDescriptions, itemTypes);
    }

    @Override
    public CompositeData map(GcMonitorSnapshot snapshot) {
        HashMap<String, Object> data = new HashMap<>();
        windowConverters.forEach((name, converter) -> data.put(name, converter.map(snapshot)));
        try {
            return new CompositeDataSupport(type, data);
        } catch (OpenDataException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public CompositeType getType() {
        return type;
    }

}
