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

import java.util.Objects;
import java.util.function.Supplier;

public class CachingSupplier<T> implements Supplier<T> {

    private final Supplier<T> targetSupplier;
    private final long cachingDurationMillis;
    private final Clock clock;

    private T cachedValue;
    private long lastSnapshotTakeTimeMillis;

    public CachingSupplier(long cachingDurationMillis, Clock clock, Supplier<T> targetSupplier) {
        if (cachingDurationMillis >= Long.MAX_VALUE / 2) {
            throw new IllegalArgumentException("Too big cachingDurationMillis");
        }
        this.targetSupplier = targetSupplier;
        this.cachingDurationMillis = cachingDurationMillis;
        this.clock = Objects.requireNonNull(clock);
        this.lastSnapshotTakeTimeMillis = -cachingDurationMillis;
    }

    @Override
    final synchronized public T get() {
        long nowMillis = clock.currentTimeMillis();
        if (nowMillis - lastSnapshotTakeTimeMillis < cachingDurationMillis) {
            return cachedValue;
        }
        cachedValue = targetSupplier.get();
        lastSnapshotTakeTimeMillis = nowMillis;
        return cachedValue;
    }

}
