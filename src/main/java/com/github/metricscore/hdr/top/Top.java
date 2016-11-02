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


import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * The top of queries sorted by its latency.
 * The top is sized, independent of count of recorded queries, the top always stores no more than {@link #getPositionCount} positions,
 * the longer queries displace shorter queries when top reaches it max size.
 */
public interface Top {

    /**
     * Registers latency of query. To avoid unnecessary memory allocation for Strings the descriptionSupplier will be called only if latency is greater then "SlowQueryThreshold"
     * and latency is greater than any other query in the top.
     *
     * @param latencyTime query duration
     * @param latencyUnit resolution of latency time
     * @param descriptionSupplier lazy supplier for query description
     *
     * @return true if latency inserted to any position.
     */
    boolean update(long latencyTime, TimeUnit latencyUnit, Supplier<String> descriptionSupplier);

    /**
     * @return the top of queries in descend order, slowest query will be at first place.
     */
    List<Position> getPositionsInDescendingOrder();

    /**
     * @return the maximum count of positions in the top.
     */
    int getPositionCount();

    /**
     * Returns slow queries threshold.
     * The queries with latency which shorter than threshold, will not be tracked in the top.
     *
     * @return slow queries threshold
     */
    long getSlowQueryThresholdNanos();

    int getMaxLengthOfQueryDescription();

}
