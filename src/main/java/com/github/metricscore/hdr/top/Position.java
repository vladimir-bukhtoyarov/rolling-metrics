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
import java.util.function.Supplier;

/**
 * Represents query latency with user friendly query description.
 */
public class Position implements Comparable<Position> {

    private final long latencyTime;
    private final TimeUnit latencyUnit;
    private final String description;
    private final long timestamp;
    private long latencyInNanoseconds;

    public Position(long timestamp, long latencyTime, TimeUnit latencyUnit, Supplier<String> descriptionSupplier, int maxDescriptionLengt) {
        this(timestamp, latencyTime, latencyUnit, combineDescriptionWithLatency(latencyTime, latencyUnit, descriptionSupplier, maxDescriptionLengt));
    }

    public Position(long timestamp, long latencyTime, TimeUnit latencyUnit, String description) {
        this.latencyTime = latencyTime;
        this.latencyUnit = latencyUnit;
        this.description = description;
        this.timestamp = timestamp;
        this.latencyInNanoseconds = latencyUnit.toNanos(latencyTime);
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

    /**
     * @return latency of query in nanoseconds
     */
    public long getLatencyInNanoseconds() {
        return latencyInNanoseconds;
    }

    /**
     * Returns timestamp in milliseconds when latency taken
     *
     * @return timestamp in milliseconds when latency taken
     */
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public int compareTo(Position other) {
        if (latencyInNanoseconds != other.latencyInNanoseconds) {
            return Long.compare(latencyInNanoseconds, other.latencyInNanoseconds);
        }
        if (timestamp != other.timestamp) {
            return Long.compare(timestamp, other.timestamp);
        }
        return description.compareTo(other.description);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Position position = (Position) o;

        if (timestamp != position.timestamp) return false;
        if (latencyInNanoseconds != position.latencyInNanoseconds) return false;
        return description.equals(position.description);

    }

    @Override
    public int hashCode() {
        int result = description.hashCode();
        result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
        result = 31 * result + (int) (latencyInNanoseconds ^ (latencyInNanoseconds >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "Position{" +
                "latencyTime=" + latencyTime +
                ", latencyUnit=" + latencyUnit +
                ", description='" + description + '\'' +
                ", timestamp=" + timestamp +
                ", latencyInNanoseconds=" + latencyInNanoseconds +
                '}';
    }

    private static String combineDescriptionWithLatency(long latencyTime, TimeUnit latencyUnit, Supplier<String> descriptionSupplier, int maxDescriptionLength) {
        String queryDescription = descriptionSupplier.get();
        if (queryDescription == null) {
            throw new IllegalArgumentException("Query queryDescription should not be null");
        }
        if (queryDescription.length() > maxDescriptionLength) {
            queryDescription = queryDescription.substring(0, maxDescriptionLength);
        }
        return queryDescription;
    }

}
