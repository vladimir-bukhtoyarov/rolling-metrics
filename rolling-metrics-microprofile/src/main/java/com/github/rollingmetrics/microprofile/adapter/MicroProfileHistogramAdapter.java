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

package com.github.rollingmetrics.microprofile.adapter;

import com.github.rollingmetrics.histogram.hdr.RollingHdrHistogram;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Snapshot;

import java.util.Objects;
import java.util.concurrent.atomic.LongAdder;

/**
 * Adapter from rolling-hdr histogram to Eclipse-MicroProfile histogram
 */
public class MicroProfileHistogramAdapter implements Histogram {

    private final LongAdder count = new LongAdder();
    private final RollingHdrHistogram target;

    public MicroProfileHistogramAdapter(RollingHdrHistogram target) {
        this.target = Objects.requireNonNull(target);
    }

    @Override
    public void update(int value) {
        update((long) value);
    }

    @Override
    public void update(long value) {
        target.update(value);
        count.increment();
    }

    @Override
    public long getCount() {
        return count.longValue();
    }

    @Override
    public Snapshot getSnapshot() {
        return new MicroProfileSnapshotAdapter(target.getSnapshot());
    }

}