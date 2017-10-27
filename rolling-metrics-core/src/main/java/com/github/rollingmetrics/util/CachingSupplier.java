/*
 *
 *  Copyright 2017 Vladimir Bukhtoyarov
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.github.rollingmetrics.util;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Supplier;

public class CachingSupplier<T> implements Supplier<T> {

    private final Supplier<T> targetSupplier;
    private final long cachingDurationNanos;
    private final Ticker ticker;

    private T cachedValue;
    private long lastSnapshotNanoTime;

    public CachingSupplier(Duration cachingDuration, Ticker ticker, Supplier<T> targetSupplier) {
        this.targetSupplier = targetSupplier;
        this.cachingDurationNanos = cachingDuration.toNanos();
        this.ticker = Objects.requireNonNull(ticker);
    }

    @Override
    final synchronized public T get() {
        long nanoTime = ticker.nanoTime();
        if (cachedValue != null && nanoTime - lastSnapshotNanoTime < cachingDurationNanos) {
            return cachedValue;
        }
        cachedValue = targetSupplier.get();
        lastSnapshotNanoTime = nanoTime;
        return cachedValue;
    }

}
