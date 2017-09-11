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

import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Snapshot;
import com.github.rollingmetrics.histogram.hdr.RollingHdrHistogram;

import java.util.Objects;

/**
 * TODO
 */
class ReservoirToRollingHdrHistogramAdapter implements Reservoir {

    private final RollingHdrHistogram target;

    public ReservoirToRollingHdrHistogramAdapter(RollingHdrHistogram target) {
        this.target = Objects.requireNonNull(target);
    }

    @Override
    public int size() {
        return getSnapshot().size();
    }

    @Override
    public void update(long value) {
        target.update(value);
    }

    @Override
    public Snapshot getSnapshot() {
        return new RollingHdrHistogramSnapshotAdapter(target.getSnapshot());
    }

}
