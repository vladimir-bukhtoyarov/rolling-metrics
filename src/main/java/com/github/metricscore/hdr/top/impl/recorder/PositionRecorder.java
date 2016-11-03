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

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 *  Is not a part of public API, this class just used as building block for other QueryTop implementations.
 */
public abstract class PositionRecorder {

    protected final int size;
    protected final long slowQueryThresholdNanos;
    protected final int maxLengthOfQueryDescription;

    protected PositionRecorder(int size, Duration slowQueryThreshold, int maxLengthOfQueryDescription) {
        this(size, slowQueryThreshold.toNanos(), maxLengthOfQueryDescription);
    }

    protected PositionRecorder(int size, long slowQueryThresholdNanos, int maxLengthOfQueryDescription) {
        this.slowQueryThresholdNanos = slowQueryThresholdNanos;
        this.size = size;
        this.maxLengthOfQueryDescription = maxLengthOfQueryDescription;
    }

    public void update(long timestamp, long latencyTime, TimeUnit latencyUnit, Supplier<String> descriptionSupplier) {
        long latencyNanos = latencyUnit.toNanos(latencyTime);
        if (latencyNanos < slowQueryThresholdNanos) {
            // the measure should be skipped because it is lesser then threshold
            return;
        }
        updateConcurrently(timestamp, latencyTime, latencyUnit, descriptionSupplier, latencyNanos);
    }

    public int getSize() {
        return size;
    }

    public static PositionRecorder createRecorder(int size, long slowQueryThresholdNanos, int maxLengthOfQueryDescription) {
        if (size == 1) {
            return new SinglePositionRecorder(slowQueryThresholdNanos, maxLengthOfQueryDescription);
        } else {
            return new MultiPositionRecorder(size, slowQueryThresholdNanos, maxLengthOfQueryDescription);
        }
    }

    public PositionRecorder createEmptyCopy() {
        return createRecorder(size, slowQueryThresholdNanos, maxLengthOfQueryDescription);
    }

    protected abstract void updateConcurrently(long timestamp, long latencyTime, TimeUnit latencyUnit, Supplier<String> descriptionSupplier, long latencyNanos);

    public abstract void reset();

    public abstract void addInto(PositionCollector collector);

    public abstract List<Position> getPositionsInDescendingOrder();

}
