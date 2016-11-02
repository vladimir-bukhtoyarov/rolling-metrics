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

package com.github.metricscore.hdr.top.basic;

import com.github.metricscore.hdr.top.Position;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Represents query latency with user friendly query description.
 */
public class PositionImpl implements Position, Supplier<String> {

    public static PositionImpl FAKE_QUERY = new PositionImpl(0, TimeUnit.SECONDS, "");

    private final long latencyTime;
    private final TimeUnit latencyUnit;
    private final String description;

    public PositionImpl(long latencyTime, TimeUnit latencyUnit, String description) {
        this.latencyTime = latencyTime;
        this.latencyUnit = latencyUnit;
        this.description = description;
    }

    @Override
    public String getQueryDescription() {
        return description;
    }

    @Override
    public long getLatencyTime() {
        return latencyTime;
    }

    @Override
    public TimeUnit getLatencyUnit() {
        return latencyUnit;
    }

    @Override
    public long getLatencyInNanoseconds() {
        return latencyUnit.toNanos(latencyTime);
    }

    @Override
    public String get() {
        return description;
    }

}
