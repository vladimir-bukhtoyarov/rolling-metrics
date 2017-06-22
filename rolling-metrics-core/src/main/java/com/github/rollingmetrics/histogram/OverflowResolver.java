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

package com.github.rollingmetrics.histogram;

/**
 * Specifies behavior which should be applied when need to write to reservoir value which greater than highestTrackableValue.
 *
 * When highestTrackableValue is specified then {@link org.HdrHistogram.AbstractHistogram} may throw ArrayIndexOutOfBoundsException in the method {@link org.HdrHistogram.AbstractHistogram#recordValue(long)}
 * if value is exceeds highestTrackableValue, so OverflowResolver is addressed to solve this problem.
 *
 * @see HdrBuilder#withHighestTrackableValue(long, OverflowResolver)
 * @see org.HdrHistogram.AbstractHistogram#recordValue(long)
 */
public enum OverflowResolver {

    /**
     * Resolves overflow by replacing the exceeded value by highestTrackableValue.
     */
    REDUCE_TO_HIGHEST_TRACKABLE,

    /**
     * Resolves overflow by skipping value which is exceeds highestTrackableValue, so exceeded value will not be stored.
     */
    SKIP,

    /**
     * Do not resolve overflow, just passes the value directly to {@link org.HdrHistogram.AbstractHistogram#recordValue(long)}
     * as result ArrayIndexOutOfBoundsException may be thrown.
     *
     * <p>Use this way with double attention <b>iff</b> you clearly understanding what you do.
     */
    PASS_THRU

}