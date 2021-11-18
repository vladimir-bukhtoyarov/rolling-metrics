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

public class SnapshotConverter implements Converter {

    static final String TYPE_DESCRIPTION = "Shows aggregated information about garbage collectors";
    static final String TYPE_NAME = "com.github.gcmonitor.snapshot";

    private final CompositeType type;
    private final SortedMap<String, CollectorConverter> collectorConverters = new TreeMap<>();

    public SnapshotConverter(GcMonitorConfiguration configuration) throws OpenDataException {
        Set<String> collectorNames = configuration.getCollectorNames();
        String[] itemNames = new String[collectorNames.size()];
        String[] itemDescriptions = new String[collectorNames.size()];
        OpenType<?>[] itemTypes = new OpenType<?>[collectorNames.size()];

        int i = 0;
        for (String collectorName : collectorNames) {
            CollectorConverter collectorConverter = new CollectorConverter(configuration, collectorName);
            collectorConverters.put(collectorName, collectorConverter);
            itemNames[i] = collectorName;
            itemDescriptions[i] = "Shows aggregated information about garbage collector [" + collectorName + "]";
            itemTypes[i] = collectorConverter.getType();
            i++;
        }

        this.type = new CompositeType(TYPE_NAME, TYPE_DESCRIPTION, itemNames, itemDescriptions, itemTypes);
    }

    public CompositeData map(GcMonitorSnapshot snapshot) {
        HashMap<String, Object> data = new HashMap<>();
        collectorConverters.forEach((name, converter) -> data.put(name, converter.map(snapshot)));
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
