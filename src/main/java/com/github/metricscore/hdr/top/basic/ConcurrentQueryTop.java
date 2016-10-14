/*
 *    Copyright 2016 Vladimir Bukhtoyarov
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package com.github.metricscore.hdr.top.basic;
import com.github.metricscore.hdr.top.LatencyWithDescription;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Is not a part of public API, this class just used as building block for other QueryTop implementations.
 *
 * This implementation supports concurrent updates, but top calculation is weakly consistent(inherited from {@link ConcurrentSkipListMap}),
 * so if weakly consistency is not enough then clients of this class should provide synchronization between reader and writers by itself.
 *
 */
class ConcurrentQueryTop extends BasicQueryTop {

    private final ConcurrentSkipListMap<Long, LatencyWithDescription> top;

    ConcurrentQueryTop(int size, Duration slowQueryThreshold) {
        super(size, slowQueryThreshold);
        this.top = new ConcurrentSkipListMap<>();
        initByFakeValues();
    }

    @Override
    protected void updateImpl(long latencyTime, TimeUnit latencyUnit, Supplier<String> descriptionSupplier, long latencyNanos) {
        if (top.firstKey() >= latencyTime) {
            // the measure should be skipped because it is lesser then smallest which already tracked in the top.
            return;
        }
        String queryDescription = combineDescriptionWithLatency(latencyTime, latencyUnit, descriptionSupplier);
        LatencyWithDescription position = new LatencyWithDescription(latencyTime, latencyUnit, queryDescription);
        top.put(latencyTime, position);

        top.pollFirstEntry();
    }

    @Override
    public List<LatencyWithDescription> getDescendingRaiting() {
        List<LatencyWithDescription> descendingTop = new ArrayList<>(size);
        for (Map.Entry<Long, LatencyWithDescription> entry : top.descendingMap().entrySet()) {
            descendingTop.add(entry.getValue());
            if (descendingTop.size() == size) {
                return descendingTop;
            }
        }
        return descendingTop;
    }

    @Override
    public void reset() {
        top.clear();
        initByFakeValues();
    }

    private void initByFakeValues() {
        for (int i = 1; i <= size; i++) {
            top.put((long) -i, FAKE_QUERY);
        }
    }

}
