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

package com.github.rollingmetrics.top.impl.collector;
import com.github.rollingmetrics.top.Position;

import java.util.*;

/**
 * Is not a part of public API, this class just used as building block for high-level Top implementations.
 *
 * This implementation does not support concurrent access at all, synchronization aspects should be managed outside.
 */
class MultiPositionCollector implements PositionCollector {

    private final TreeSet<Position> positions = new TreeSet<>();
    private final int maxSize;

    MultiPositionCollector(int maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    public boolean add(Position position) {
        if (positions.size() < maxSize) {
            positions.add(position);
            return true;
        }

        Position min = positions.first();
        if (PositionCollector.isNeedToAdd(position, min)) {
            if (positions.add(position)) {
                positions.remove(min);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public void addInto(PositionCollector other) {
        for (Position position: positions.descendingSet()) {
            if (!other.add(position)) {
                return;
            }
        }
    }

    @Override
    public void reset() {
        positions.clear();
    }

    @Override
    public List<Position> getPositionsInDescendingOrder() {
        if (positions.isEmpty()) {
            return Collections.emptyList();
        }
        ArrayList<Position> result = new ArrayList<>(positions.size());
        result.addAll(positions.descendingSet());
        return result;
    }

    @Override
    public String toString() {
        return "MultiPositionCollector{" +
                "positions=" + positions +
                ", maxSize=" + maxSize +
                '}';
    }

}
