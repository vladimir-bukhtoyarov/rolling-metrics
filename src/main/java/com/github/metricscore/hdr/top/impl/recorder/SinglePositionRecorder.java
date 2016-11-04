/*
 *
 *  Copyright 2016 Vladimir Bukhtoyarov
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

package com.github.metricscore.hdr.top.impl.recorder;

import com.github.metricscore.hdr.top.Position;
import com.github.metricscore.hdr.top.impl.collector.PositionCollector;

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
class SinglePositionRecorder extends PositionRecorder {

    private final AtomicReference<Position> max;

    public SinglePositionRecorder(long slowQueryThresholdNanos, int maxDescriptionLength) {
        super(1, slowQueryThresholdNanos, maxDescriptionLength);
        this.max = new AtomicReference<>(null);
    }

    @Override
    protected void updateConcurrently(long timestamp, long latencyTime, TimeUnit latencyUnit, Supplier<String> descriptionSupplier, long latencyNanos) {
        Position newMax = null;
        while (true) {
            Position previousMax = max.get();
            if (!isNeedToAdd(timestamp, latencyNanos, previousMax)) {
                return;
            }
            if (newMax == null) {
                newMax = new Position(timestamp, latencyTime, latencyUnit, descriptionSupplier, super.maxDescriptionLength);
            }
            if (max.compareAndSet(previousMax, newMax)) {
                return;
            }
        }
    }

    @Override
    public List<Position> getPositionsInDescendingOrder() {
        return Collections.singletonList(max.get());
    }

    @Override
    public void reset() {
        max.set(null);
    }

    @Override
    public void addInto(PositionCollector collector) {
        Position position = max.get();
        if (position != null) {
            collector.add(position);
        }
    }

    @Override
    public String toString() {
        return "SinglePositionRecorder{" +
                "max=" + max +
                '}';
    }

}
