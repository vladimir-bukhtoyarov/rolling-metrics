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

package io.github.gcmonitor.stat;


import java.util.Map;

public class GcMonitorSnapshot {

    private final Map<String, Map<String, CollectorWindowSnapshot>> data;

    public GcMonitorSnapshot(Map<String, Map<String, CollectorWindowSnapshot>> data) {
        this.data = data;
    }

    public CollectorWindowSnapshot getCollectorWindowSnapshot(String collectorName, String windowName) {
        Map<String, CollectorWindowSnapshot> collectorWindows = data.get(collectorName);
        if (collectorWindows == null) {
            throw new IllegalArgumentException("Unknown collector name [" + collectorName + "]");
        }

        CollectorWindowSnapshot windowSnapshot = collectorWindows.get(windowName);
        if (windowSnapshot == null) {
            throw new IllegalArgumentException("Unknown name of collector window [" + windowName + "]");
        }
        return windowSnapshot;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        data.forEach((collectorName, collectorWindows) -> {
            collectorWindows.forEach((windowName, windowSnapshot) -> {
                sb.append(collectorName).append("-").append(windowName).append("-") .append(windowSnapshot).append("\n");
            });
        });
        return sb.toString();
    }

}
