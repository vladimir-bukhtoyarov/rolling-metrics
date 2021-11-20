/*
 *    Copyright 2017 Vladimir Bukhtoyarov
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

package com.github.rollingmetrics.histogram.hdr;

/**
 * The histogram with rolling time window retention.
 */
public interface RollingHdrHistogram {

    /**
     * Creates new instance of histogram builder
     *
     * @return new instance of histogram builder
     */
    static RollingHdrHistogramBuilder builder() {
        return new RollingHdrHistogramBuilder();
    }

    /**
     * Provide a (conservatively high) estimate of the Reservoir's total footprint in bytes
     *
     * @return a (conservatively high) estimate of the Reservoir's total footprint in bytes
     */
    int getEstimatedFootprintInBytes();

    /**
     * Takes immutable snapshot of this histogram
     *
     * @return the immutable snapshot of this histogram
     */
    RollingSnapshot getSnapshot();

    /**
     * Records value into this histogram
     *
     * @param value the number to record
     *
     * @throws IllegalArgumentException if number < 0
     */
    void update(long value);

}
