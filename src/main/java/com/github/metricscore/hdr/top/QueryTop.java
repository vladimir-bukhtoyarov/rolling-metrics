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

import com.github.metricscore.hdr.Clock;
import com.github.metricscore.hdr.top.basic.SingletonTop;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 *
 *
 */
public interface QueryTop {

    LatencyWithDescription FAKE_QUERY = new LatencyWithDescription(0, TimeUnit.SECONDS, "");

    /**
     * Registers latency of query. To avoid unnecessary memory allocation for Strings the descriptionSupplier will be called only if latency is greater then "SlowQueryThreshold"
     * and latency is greater than any other query in the top.
     *
     * @param latencyTime query duration
     * @param latencyUnit resolution of latency time
     * @param descriptionSupplier lazy supplier for query description
     */
    void update(long latencyTime, TimeUnit latencyUnit, Supplier<String> descriptionSupplier);

    /**
     * Return the top of queries in descend order, slowest query will be at first place.
     * The resulted list has always size which equals to {@link #getSize()},
     * if count of tracked queries is less than {@link #getSize()} then tail of resulted list will be populated by {@link #FAKE_QUERY}
     *
     * descending order
     * ascending order
     */
    List<LatencyWithDescription> getDescendingRating();

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

    static QueryTop createUniformTop(int size, Duration slowQueryThreshold) {
        if (size == 1) {
            return new SingletonTop(slowQueryThreshold);
        }
        return new UniformQueryTop(size, slowQueryThreshold);
    }

    static QueryTop createResetOnSnapshotTop(int size, Duration slowQueryThreshold) {
        return new ResetOnSnapshotQueryTop(size, slowQueryThreshold);
    }

    static QueryTop createResetPeriodicallyTop(int size, Duration slowQueryThreshold, Duration resetInterval) {
        return new ResetPeriodicallyQueryTop(size, slowQueryThreshold, resetInterval, Clock.defaultClock());
    }

    static QueryTop createResetByChunkTop(int size, Duration slowQueryThreshold, Duration rollingWindow, int numberChunks) {
        // TODO
        throw new UnsupportedOperationException();
    }

}
