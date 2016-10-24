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

import com.github.metricscore.hdr.top.Top;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 *  Is not a part of public API, this class just used as building block for other QueryTop implementations.
 */
public abstract class BasicTop implements Top {

    // limit to prevent user to kill performance by mistake
    private static final int MAX_SIZE = 1000;

    protected final int size;
    protected final long slowQueryThresholdNanos;

    protected BasicTop(int size, Duration slowQueryThreshold) {
        this(size, slowQueryThreshold.toNanos());
    }

    protected BasicTop(int size, long slowQueryThresholdNanos) {
        if (size > MAX_SIZE) {
            throw new IllegalArgumentException("size should be <= " + MAX_SIZE);
        }
        if (slowQueryThresholdNanos < 0) {
            throw new IllegalArgumentException("slowQueryThreshold should be positive");
        }
        this.slowQueryThresholdNanos = slowQueryThresholdNanos;
        if (size <= 0) {
            throw new IllegalArgumentException("size should be >0");
        }
        this.size = size;
    }

    @Override
    public void update(long latencyTime, TimeUnit latencyUnit, Supplier<String> descriptionSupplier) {
        long latencyNanos = latencyUnit.toNanos(latencyTime);
        if (latencyNanos < slowQueryThresholdNanos) {
            // the measure should be skipped because it is lesser then threshold
            return;
        }
        updateImpl(latencyTime, latencyUnit, descriptionSupplier, latencyNanos);
    }

    @Override
    public int getPositionCount() {
        return size;
    }

    @Override
    public long getSlowQueryThresholdNanos() {
        return slowQueryThresholdNanos;
    }

    protected abstract void updateImpl(long latencyTime, TimeUnit latencyUnit, Supplier<String> descriptionSupplier, long latencyNanos);

    static String combineDescriptionWithLatency(long latencyTime, TimeUnit latencyUnit, Supplier<String> descriptionSupplier) {
        String queryDescription = descriptionSupplier.get();
        if (queryDescription == null) {
            throw new NullPointerException("Query queryDescription should not be null");
        }
        return "" + latencyTime + " " + latencyUnit.toString() + " was spent to execute: " + queryDescription;
    }

}
