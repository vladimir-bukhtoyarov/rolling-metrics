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

package com.github.metricscore.hdr.top.impl.collector;

import com.github.metricscore.hdr.top.Position;

import java.util.Collections;
import java.util.List;

/**
 * Is not a part of public API, this class just used as building block for high-level Top implementations.
 *
 * This implementation does not support concurrent access at all, synchronization aspects must be managed outside.
 */
class SinglePositionCollector implements PositionCollector {

    private Position max;

    @Override
    public boolean add(Position position) {
        if (max == null || position.getLatencyInNanoseconds() > max.getLatencyInNanoseconds()) {
            this.max = position;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public List<Position> getPositionsInDescendingOrder() {
        if (max == null) {
            return Collections.emptyList();
        } else {
            return Collections.singletonList(max);
        }
    }

    @Override
    public void reset() {
        max = null;
    }

    @Override
    public void addInto(PositionCollector other) {
        if (max != null) {
            other.add(max);
        }
    }

    @Override
    public String toString() {
        return "SinglePositionCollector{" +
                "max=" + max +
                '}';
    }

}
