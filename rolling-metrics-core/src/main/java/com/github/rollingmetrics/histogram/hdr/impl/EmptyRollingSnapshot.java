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

package com.github.rollingmetrics.histogram.hdr.impl;


import com.github.rollingmetrics.histogram.hdr.RollingSnapshot;

public class EmptyRollingSnapshot implements RollingSnapshot {

    public static final EmptyRollingSnapshot INSTANCE = new EmptyRollingSnapshot();
    private static final long[] VALUES = new long[0];

    @Override
    public double getValue(double quantile) {
        return 0;
    }

    @Override
    public long[] getValues() {
        return VALUES;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public double getMedian() {
        return 0;
    }

    @Override
    public long getMax() {
        return 0;
    }

    @Override
    public double getMean() {
        return 0;
    }

    @Override
    public long getMin() {
        return 0;
    }

    @Override
    public double getStdDev() {
        return 0;
    }

}
