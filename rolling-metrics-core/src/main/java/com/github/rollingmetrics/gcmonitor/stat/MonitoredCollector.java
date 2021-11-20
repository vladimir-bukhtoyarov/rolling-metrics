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

import javax.management.Notification;
import java.lang.management.GarbageCollectorMXBean;
import java.util.Optional;

public final class MonitoredCollector {

    private final GarbageCollectorMXBean collectorMbean;
    private final CollectorStatistics collectorStatistics;
    private final CollectorStatistics[] allStats;
    private final CollectorSnapshot snapshot;

    MonitoredCollector(GarbageCollectorMXBean collectorMbean, Optional<CollectorStatistics> aggregatedStatistics, CollectorStatistics thisCollectorStatistics) {
        this.collectorMbean = collectorMbean;
        this.collectorStatistics = thisCollectorStatistics;

        if (aggregatedStatistics.isPresent()) {
            this.allStats = new CollectorStatistics[] {aggregatedStatistics.get(), collectorStatistics};
        } else {
            this.allStats = new CollectorStatistics[] {collectorStatistics};
        }
        this.snapshot = new CollectorSnapshot(collectorMbean);
    }

    public void handleNotification(Notification notification) {
        long collectionTimeMillis = collectorMbean.getCollectionTime();
        long collectionCount = collectorMbean.getCollectionCount();

        long collectionTimeDeltaMillis = collectionTimeMillis - snapshot.getCollectionTimeMillis();
        long collectionCountDelta = collectionCount - snapshot.getCollectionCount();
        if (collectionCountDelta == 0) {
            return;
        }
        for (CollectorStatistics stat : allStats) {
            stat.update(collectionTimeDeltaMillis, collectionCountDelta);
        }
        snapshot.update(collectionTimeMillis, collectionCount);
    }

}
