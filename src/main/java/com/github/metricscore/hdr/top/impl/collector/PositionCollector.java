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

import java.util.List;

/**
 * Is not a part of public API, this class just used as building block for high-level Top implementations.
 */
public interface PositionCollector {

    boolean add(Position position);

    void addInto(PositionCollector other);

    void reset();

    List<Position> getPositionsInDescendingOrder();

    static PositionCollector createCollector(int size) {
        if (size == 1) {
            return new SinglePositionCollector();
        } else {
            return new MultiPositionCollector(size);
        }
    }

}
