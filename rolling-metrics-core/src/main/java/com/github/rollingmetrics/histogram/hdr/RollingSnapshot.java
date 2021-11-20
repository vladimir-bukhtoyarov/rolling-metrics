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
 * Represents immutable statistics about histogram
 */
public interface RollingSnapshot {

    /**
     * Get the value at a given percentile.
     *
     * @param quantile the number between 0 and 1
     *
     * @return the value at a given percentile.
     */
    double getValue(double quantile);

    /**
     * Depending on whether optimization of percentiles was configured this method can return
     * <ul>
     *     <li>Value for each configured quantile when optimization of percentiles is configured</li>
     *     <li>All recorded values(but without duplicates) when optimization of percentiles is switched-off</li>
     * </ul>
     *
     * @return recorded values
     */
    long[] getValues();

    /**
     * Get median from recorded values
     *
     * @return the median from recorded values
     */
    double getMedian();

    /**
     * Get maximum from recorded values
     *
     * @return the maximum from recorded values
     */
    long getMax();

    /**
     * Get average from recorded values
     *
     * @return the average from recorded values
     */
    double getMean();

    /**
     * Get minimum from recorded values
     *
     * @return the minimum from recorded values
     */
    long getMin();

    double getStdDev();

    /**
     * Get the total count of all recorded values in the histogram
     *
     * @return the total count of all recorded values in the histogram
     */
    long getSamplesCount();

}
