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

package com.github.rollingmetrics.top;

import java.util.Objects;

/**
 * Represents position inside ranking.
 */
public class Position {

    private final long weight;
    private final Object identity;

    public Position(long weight, Object identity) {
        this.weight = weight;
        this.identity = Objects.requireNonNull(identity);
    }

    public long getWeight() {
        return weight;
    }

    public Object getIdentity() {
        return identity;
    }

    @Override
    public String toString() {
        return "Position{" +
                "weight=" + weight +
                ", identity=" + identity +
                '}';
    }

}
