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

import java.util.SortedMap;

public class CollectorStatistics {

    private final SortedMap<String, CollectorWindow> windows;

    CollectorStatistics(SortedMap<String, CollectorWindow> windows) {
        this.windows = windows;
    }

    SortedMap<String, CollectorWindow> getWindows() {
        return windows;
    }

    void update(long collectionTimeDeltaMillis, long collectionCountDelta) {
        for (CollectorWindow window : windows.values()) {
            window.update(collectionTimeDeltaMillis, collectionCountDelta);
        }
    }

}
