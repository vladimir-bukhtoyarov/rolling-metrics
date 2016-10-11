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
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;


abstract class AbstractQueryTop implements QueryTop {

    protected final int size;
    protected final long slowQueryThresholdNanos;

    protected AbstractQueryTop(int size, Duration slowQueryThreshold) {
        if (size <= 0) {
            throw new IllegalArgumentException("size should be >0");
        }
        this.size = size;
        if (slowQueryThreshold.isNegative()) {
            throw new IllegalArgumentException("slowQueryThreshold should be positive");
        }
        this.slowQueryThresholdNanos = slowQueryThreshold.toNanos();
    }

    @Override
    public void update(long latencyTime, TimeUnit latencyUnit, Supplier<String> descriptionSupplier) {
        long latencyNanos = latencyUnit.toNanos(latencyTime);
        if (latencyNanos < slowQueryThresholdNanos) {
            return;
        }
        updateImpl(latencyNanos, latencyTime, latencyUnit, descriptionSupplier);
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public long getSlowQueryThresholdNanos() {
        return slowQueryThresholdNanos;
    }

    protected abstract void updateImpl(long latencyNanos, long latencyTime, TimeUnit latencyUnit, Supplier<String> descriptionSupplier);

    protected static LatencyWithDescription[] tryInsert(long latencyNanos, long latencyTime, TimeUnit latencyUnit, Supplier<String> descriptionSupplier, LatencyWithDescription[] oldLatencies) {
        if (oldLatencies[0] == null) {
            LatencyWithDescription[] newLatencies = new LatencyWithDescription[oldLatencies.length];
            String queryDescription = combineDescriptionWithLatency(latencyTime, latencyUnit, descriptionSupplier);
            newLatencies[0] = new LatencyWithDescription(latencyTime, latencyUnit, queryDescription);
            return newLatencies;
        }
        if (oldLatencies[0].getLatencyInNanoseconds() > latencyNanos) {
            return oldLatencies;
        }
        // TODO

        return null;
    }

    private static String combineDescriptionWithLatency(long latencyTime, TimeUnit latencyUnit, Supplier<String> descriptionSupplier) {
        String queryDescription = descriptionSupplier.get();
        if (queryDescription == null) {
            throw new NullPointerException("Query queryDescription should not be null");
        }
        return "" + latencyTime + " " + latencyUnit.toString() + " was spent to execute: " + queryDescription;
    }

}
