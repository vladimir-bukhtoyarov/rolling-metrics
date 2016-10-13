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

package com.github.metricscore.hdr.top;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Is not a part of public API, this class just used as building block for other QueryTop implementations.
 *
 * This implementation supports concurrent updates, but top calculation is weakly consistent(inherited from {@link ConcurrentSkipListMap}),
 * so if weakly consistency is not enough then clients of this class should provide synchronization between reader and writers by itself.
 *
 */
class ConcurrentQueryTop implements QueryTop {

    private final int size;
    private final long slowQueryThresholdNanos;
    private final ConcurrentSkipListMap<PositionKey, LatencyWithDescription> top;

    /*
      Auxiliary sequence used in comparision of PositionKey
      which provide guarantee that in case of equal latency fresh measures will have greater weight than older measures.
     */
    private final AtomicLong sequence;

    ConcurrentQueryTop(int size, Duration slowQueryThreshold) {
        if (size <= 0) {
            throw new IllegalArgumentException("size should be >0");
        }
        this.size = size;
        if (slowQueryThreshold.isNegative()) {
            throw new IllegalArgumentException("slowQueryThreshold should be positive");
        }
        this.slowQueryThresholdNanos = slowQueryThreshold.toNanos();

        sequence = new AtomicLong();

        top = new ConcurrentSkipListMap<>();
        for (int i = 0; i < size; i++) {
            PositionKey key = new PositionKey(-1, sequence.incrementAndGet());
            top.put(key, FAKE_QUERY);
        }
    }

    @Override
    public void update(long latencyTime, TimeUnit latencyUnit, Supplier<String> descriptionSupplier) {
        long latencyNanos = latencyUnit.toNanos(latencyTime);
        if (latencyNanos < slowQueryThresholdNanos) {
            // the measure should be skipped because it is lesser then threshold
            return;
        }
        if (top.firstKey().nanoseconds > latencyTime) {
            // the measure should be skipped because it is lesser then smallest which already tracked in the top.
            return;
        }

        String queryDescription = combineDescriptionWithLatency(latencyTime, latencyUnit, descriptionSupplier);
        PositionKey positionKey = new PositionKey(latencyNanos, sequence.incrementAndGet());
        LatencyWithDescription position = new LatencyWithDescription(latencyTime, latencyUnit, queryDescription);
        top.put(positionKey, position);

        top.pollFirstEntry();
    }

    @Override
    public List<LatencyWithDescription> getDescendingRaiting() {
        List<LatencyWithDescription> descendingTop = new ArrayList<>(size);
        for (Map.Entry<PositionKey, LatencyWithDescription> entry : top.descendingMap().entrySet()) {
            descendingTop.add(entry.getValue());
            if (descendingTop.size() == size) {
                return descendingTop;
            }
        }
        return descendingTop;
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public long getSlowQueryThresholdNanos() {
        return slowQueryThresholdNanos;
    }

    private static String combineDescriptionWithLatency(long latencyTime, TimeUnit latencyUnit, Supplier<String> descriptionSupplier) {
        String queryDescription = descriptionSupplier.get();
        if (queryDescription == null) {
            throw new NullPointerException("Query queryDescription should not be null");
        }
        return "" + latencyTime + " " + latencyUnit.toString() + " was spent to execute: " + queryDescription;
    }

    private static class PositionKey implements Comparable<PositionKey> {

        private final long nanoseconds;
        private final long sequence;

        PositionKey(long nanoseconds, long sequence) {
            this.nanoseconds = nanoseconds;
            this.sequence = sequence;
        }

        @Override
        public int compareTo(PositionKey other) {
            if (nanoseconds != other.nanoseconds) {
                return Long.compare(nanoseconds, other.nanoseconds);
            }
            return Long.compare(sequence, other.sequence);
        }
    }

}
