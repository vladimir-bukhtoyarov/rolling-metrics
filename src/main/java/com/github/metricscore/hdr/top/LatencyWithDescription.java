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

package com.github.metricscore.hdr.top;

import java.util.concurrent.TimeUnit;

/**
 * Represents query latency with user friendly query description.
 */
public class LatencyWithDescription {

    private final long latencyTime;
    private final TimeUnit latencyUnit;
    private final String description;

    public LatencyWithDescription(long latencyTime, TimeUnit latencyUnit, String description) {
        this.latencyTime = latencyTime;
        this.latencyUnit = latencyUnit;
        this.description = description;
    }

    /**
     * @return user friendly query description. For example SQL or HTTP URL.
     */
    public String getQueryDescription() {
        return description;
    }

    /**
     * @return the latency of query, resolution of latency time unit can be get via {@link #getLatencyUnit()}
     */
    public long getLatencyTime() {
        return latencyTime;
    }

    /**
     * @return time units in which latency was measured.
     */
    public TimeUnit getLatencyUnit() {
        return latencyUnit;
    }

    long getLatencyInNanoseconds() {
        return latencyUnit.toNanos(latencyTime);
    }

}
