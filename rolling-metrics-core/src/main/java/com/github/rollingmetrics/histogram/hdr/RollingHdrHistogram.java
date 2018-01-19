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
 * The wrapper around {@link org.HdrHistogram HdrHistogram} that allows to customize retention policy.
 *
 * @see org.HdrHistogram
 */
public interface RollingHdrHistogram {

    /**
     * Provide a (conservatively high) estimate of this histogram total footprint in bytes
     *
     * @return a (conservatively high) estimate of this histogram total footprint in bytes
     */
    int getEstimatedFootprintInBytes();

    /**
     * Returns snapshot of values recorded to this histogram
     *
     * @return snapshot of values recorded to this histogram
     */
    RollingSnapshot getSnapshot();

    /**
     * Record a value in the histogram
     *
     * @param value The value to be recorded
     */
    void update(long value);

}
