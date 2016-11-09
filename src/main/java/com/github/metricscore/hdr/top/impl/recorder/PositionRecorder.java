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

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 *  Is not a part of public API, this class just used as building block for other QueryTop implementations.
 */
public abstract class PositionRecorder {

    protected final int size;
    protected final long latencyThresholdNanos;
    protected final int maxDescriptionLength;

    protected PositionRecorder(int size, long latencyThresholdNanos, int maxDescriptionLength) {
        this.latencyThresholdNanos = latencyThresholdNanos;
        this.size = size;
        this.maxDescriptionLength = maxDescriptionLength;
    }

    public void update(long timestamp, long latencyTime, TimeUnit latencyUnit, Supplier<String> descriptionSupplier) {
        long latencyNanos = latencyUnit.toNanos(latencyTime);
        if (latencyNanos < latencyThresholdNanos) {
            // the measure should be skipped because it is lesser then threshold
            return;
        }
        updateConcurrently(timestamp, latencyTime, latencyUnit, descriptionSupplier, latencyNanos);
    }

    public int getSize() {
        return size;
    }

    public static PositionRecorder createRecorder(int size, long latencyThresholdNanos, int maxDescriptionLength) {
        if (size == 1) {
            return new SinglePositionRecorder(latencyThresholdNanos, maxDescriptionLength);
        } else {
            return new MultiPositionRecorder(size, latencyThresholdNanos, maxDescriptionLength);
        }
    }

    public PositionRecorder createEmptyCopy() {
        return createRecorder(size, latencyThresholdNanos, maxDescriptionLength);
    }

    protected boolean isNeedToAdd(long newTimestamp, long newLatency, Position currentMinimum) {
        if (currentMinimum == null) {
            return true;
        }
        if (newLatency > currentMinimum.getLatencyInNanoseconds()) {
            return true;
        }
        if (newLatency == currentMinimum.getLatencyInNanoseconds() && newTimestamp > currentMinimum.getTimestamp()) {
            return true;
        }
        return false;
    }

    protected abstract void updateConcurrently(long timestamp, long latencyTime, TimeUnit latencyUnit, Supplier<String> descriptionSupplier, long latencyNanos);

    public abstract void reset();

    public abstract void addInto(PositionCollector collector);

    public abstract List<Position> getPositionsInDescendingOrder();

}
