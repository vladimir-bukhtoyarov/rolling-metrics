/*
 *
 *  Copyright 2016 Vladimir Bukhtoyarov
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
package com.github.metricscore.hdr.histogram.util;

import com.codahale.metrics.Snapshot;

import java.io.OutputStream;

public class EmptySnapshot extends Snapshot {

    public static final EmptySnapshot INSTANCE = new EmptySnapshot();
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

    @Override
    public void dump(OutputStream output) {
        return;
    }

}
