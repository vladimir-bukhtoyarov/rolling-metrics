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

import com.github.rollingmetrics.retention.RetentionPolicy;

/**
 * TODO
 */
public interface RollingHdrHistogram {

    /**
     * Provide a (conservatively high) estimate of the Reservoir's total footprint in bytes
     *
     * @return a (conservatively high) estimate of the Reservoir's total footprint in bytes
     */
    int getEstimatedFootprintInBytes();

    RollingSnapshot getSnapshot();

    void update(long value);

}
