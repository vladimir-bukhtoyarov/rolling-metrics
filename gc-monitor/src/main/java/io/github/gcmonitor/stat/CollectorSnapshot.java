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

import java.lang.management.GarbageCollectorMXBean;

public class CollectorSnapshot {

    private long collectionTimeMillis;
    private long collectionCount;

    public CollectorSnapshot(GarbageCollectorMXBean collectorMbean) {
        this.collectionCount = collectorMbean.getCollectionCount();
        this.collectionTimeMillis = collectorMbean.getCollectionTime();
    }

    public void update(long collectionTimeMillis, long collectionCount) {
        this.collectionTimeMillis = collectionTimeMillis;
        this.collectionCount = collectionCount;
    }

    public long getCollectionCount() {
        return collectionCount;
    }

    public long getCollectionTimeMillis() {
        return collectionTimeMillis;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CollectorSnapshot{");
        sb.append("collectionTimeMillis=").append(collectionTimeMillis);
        sb.append(", collectionCount=").append(collectionCount);
        sb.append('}');
        return sb.toString();
    }

}
