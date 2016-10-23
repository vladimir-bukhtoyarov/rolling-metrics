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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Is not a part of public API, this class just used as building block for other QueryTop implementations.
 *
 * Special implementation for top with size 1
 *
 */
public class SingletonTop extends BasicTop implements ComposableTop<SingletonTop> {

    private final AtomicReference<LatencyWithDescription> max;

    public SingletonTop(Duration slowQueryThreshold) {
        this(slowQueryThreshold.toNanos());
    }

    public SingletonTop(long slowQueryThresholdNanos) {
        super(1, slowQueryThresholdNanos);
        this.max = new AtomicReference<>(FAKE_QUERY);
    }

    @Override
    protected void updateImpl(long latencyTime, TimeUnit latencyUnit, Supplier<String> descriptionSupplier, long latencyNanos) {
        LatencyWithDescription newMax = null;
        while (true) {
            LatencyWithDescription previousMax = max.get();
            if (latencyNanos <= previousMax.getLatencyInNanoseconds()) {
                return;
            }
            if (newMax == null) {
                String description = combineDescriptionWithLatency(latencyTime, latencyUnit, descriptionSupplier);
                newMax = new LatencyWithDescription(latencyTime, latencyUnit, description);
            }
            if (max.compareAndSet(previousMax, newMax)) {
                return;
            }
        }
    }

    @Override
    public List<LatencyWithDescription> getDescendingRating() {
        return Collections.singletonList(max.get());
    }

    @Override
    public void reset() {
        max.set(FAKE_QUERY);
    }

    @Override
    public void add(SingletonTop other) {
        LatencyWithDescription otherLatency = other.max.get();
        if (max.get().getLatencyInNanoseconds() < otherLatency.getLatencyInNanoseconds()) {
            max.set(otherLatency);
        }
    }

    @Override
    public SingletonTop createEmptyCopy() {
        return new SingletonTop(super.slowQueryThresholdNanos);
    }

}
