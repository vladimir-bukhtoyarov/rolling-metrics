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

package com.github.rollingmetrics.dropwizard.adapter;

import com.codahale.metrics.Snapshot;
import com.github.rollingmetrics.histogram.hdr.RollingSnapshot;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * The adapter for {@link com.codahale.metrics.Reservoir}
 */
public class RollingHdrHistogramSnapshotAdapter extends Snapshot {

    private final RollingSnapshot target;

    public RollingHdrHistogramSnapshotAdapter(RollingSnapshot target) {
        this.target = Objects.requireNonNull(target);
    }

    @Override
    public double getValue(double quantile) {
        return target.getValue(quantile);
    }

    @Override
    public long[] getValues() {
        return target.getValues();
    }

    @Override
    public int size() {
        return getValues().length;
    }

    @Override
    public long getMax() {
        return target.getMax();
    }

    @Override
    public double getMean() {
        return target.getMean();
    }

    @Override
    public long getMin() {
        return target.getMin();
    }

    @Override
    public double getStdDev() {
        return target.getStdDev();
    }

    @Override
    public void dump(OutputStream output) {
        try (PrintWriter p = new PrintWriter(new OutputStreamWriter(output, UTF_8))) {
            for (long value : getValues()) {
                p.printf("%f%n", (double) value);
            }
        }
    }

}
