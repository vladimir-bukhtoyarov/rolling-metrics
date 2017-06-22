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

package com.github.rollingmetrics.top.impl.recorder;
import com.github.rollingmetrics.top.Position;
import com.github.rollingmetrics.top.impl.collector.PositionCollector;

import java.util.ArrayList;
import java.util.Collections;
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
class MultiPositionRecorder extends PositionRecorder {

    private final ConcurrentSkipListMap<PositionKey, Position> positions;
    private final AtomicLong phaseSequence = new AtomicLong();

    MultiPositionRecorder(int size, long latencyThresholdNanos, int maxDescriptionLength) {
        super(size, latencyThresholdNanos, maxDescriptionLength);
        this.positions = new ConcurrentSkipListMap<>();

        // init by fake values
        long phase = phaseSequence.get();
        for (int i = 1; i <= size; i++) {
            Position fake = new Position(0, -i, TimeUnit.NANOSECONDS, "");
            positions.put(new PositionKey(phase, fake), fake);
        }
    }

    @Override
    protected void updateConcurrently(long timestamp, long latencyTime, TimeUnit latencyUnit, Supplier<String> descriptionSupplier, long latencyNanos) {
        Map.Entry<PositionKey, Position> firstEntry = positions.firstEntry();
        PositionKey firstKey = firstEntry.getKey();
        Position firstPosition = firstEntry.getValue();
        long currentPhase = phaseSequence.get();
        if (!isNeedToAdd(timestamp, latencyNanos, firstPosition, firstKey, currentPhase)) {
            return;
        }
        Position position = new Position(timestamp, latencyTime, latencyUnit, descriptionSupplier, maxDescriptionLength);
        if (positions.putIfAbsent(new PositionKey(currentPhase, position), position) == null) {
            positions.pollFirstEntry();
        }
    }

    @Override
    public List<Position> getPositionsInDescendingOrder() {
        List<Position> descendingTop = null;
        long currentPhase = phaseSequence.get();

        for (Map.Entry<PositionKey, Position> entry : positions.descendingMap().entrySet()) {
            Position position = entry.getValue();
            if (currentPhase != entry.getKey().phase) {
                return notNullList(descendingTop);
            }
            if (isFake(position)) {
                return notNullList(descendingTop);
            }
            if (descendingTop == null) {
                descendingTop = new ArrayList<>(size);
            }
            descendingTop.add(position);
        }
        return notNullList(descendingTop);
    }

    @Override
    public void reset() {
        // increasing phase will invalidate all recorded values for rating calculation,
        // so touching ConcurrentSkipListMap is not needed
        phaseSequence.incrementAndGet();
    }

    @Override
    public void addInto(PositionCollector collector) {
        long currentPhase = this.phaseSequence.get();
        for(Map.Entry<PositionKey, Position> positionEntry: positions.descendingMap().entrySet()) {
            PositionKey key = positionEntry.getKey();
            if (key.phase != currentPhase) {
                return;
            }
            Position position = positionEntry.getValue();
            if (isFake(position)) {
                return;
            }
            if (!collector.add(position)) {
                return;
            }
        }
    }

    private boolean isNeedToAdd(long newTimestamp, long newLatencyNanos, Position firstPosition, PositionKey firstKey, long currentPhase) {
        if (firstKey.phase != currentPhase) {
            return true;
        }
        return super.isNeedToAdd(newTimestamp, newLatencyNanos, firstPosition);
    }

    private boolean isFake(Position position) {
        return position.getLatencyInNanoseconds() < 0;
    }

    private List<Position> notNullList(List<Position> list) {
        return list == null? Collections.emptyList(): list;
    }

    private final class PositionKey implements Comparable<PositionKey> {
        final long phase;
        final Position position;

        public PositionKey(long phase, Position position) {
            this.phase = phase;
            this.position = position;
        }

        @Override
        public int compareTo(PositionKey other) {
            if (phase != other.phase) {
                // To compare two phase values f1 and f2 we should use f2 - f1 > 0, not f2 > f1,
                // because of the possibility of numerical overflow of AtomicLong.
                if (phase - other.phase > 0) {
                    return 1;
                } else {
                    return -1;
                }
            }
            return position.compareTo(other.position);
        }
    }

    @Override
    public String toString() {
        return "MultiPositionRecorder{" +
                "positions=" + positions +
                ", phaseSequence=" + phaseSequence +
                '}';
    }

}
