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
 *
 *
 */
public interface QueryTop {

    /**
     * Registers latency of query.
     *
     * @param latencyTime duration
     * @param latencyUnit duration unit
     * @param lazySupplierForUserFriendlyQueryRepresentation supplier of query description which called if latency is greater then "SlowQueryThreshold"
     *                                                       and latency is greater than any other query in the top.
     */
    void update(long latencyTime, TimeUnit latencyUnit, Supplier<String> lazySupplierForUserFriendlyQueryRepresentation);

    /**
     * Return the top of slow queries. The key - is duration, the value is a query(for example SQL or URL)
     */
    List<LatencyWithDescription> getTop();

    /**
     * @return the maximum count of queries in the top.
     */
    int getSize();

    /**
     * Returns slow queries threshold.
     * The queries which shorter than threshold will not be tracked in the top, as result we pay nothing when all going well.
     *
     * @return slow queries threshold
     */
    long getSlowQueryThresholdNanos();

    interface DescriptionSupplier {

        String getDescription(long latencyTime, TimeUnit latencyUnit);

    }

    interface LatencyWithDescription {

        String getQueryDescription();

        long getLatencyTime();

        TimeUnit getLatencyUnit();

    }

}
