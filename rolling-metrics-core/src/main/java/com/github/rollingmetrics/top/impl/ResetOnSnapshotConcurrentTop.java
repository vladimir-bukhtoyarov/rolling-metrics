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

package com.github.rollingmetrics.top.impl;


import com.github.rollingmetrics.top.Position;
import com.github.rollingmetrics.top.Top;
import com.github.rollingmetrics.top.TopRecorderSettings;
import com.github.rollingmetrics.top.impl.recorder.PositionRecorder;
import com.github.rollingmetrics.top.impl.recorder.TwoPhasePositionRecorder;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

class ResetOnSnapshotConcurrentTop implements Top {

    private final TwoPhasePositionRecorder recorder;
    private PositionRecorder intervalRecorder;

    ResetOnSnapshotConcurrentTop(TopRecorderSettings settings) {
        this.recorder = new TwoPhasePositionRecorder(settings.getSize(), settings.getLatencyThreshold().toNanos(), settings.getMaxDescriptionLength());
        this.intervalRecorder = recorder.getIntervalRecorder();
    }

    @Override
    public void update(long timestamp, long latencyTime, TimeUnit latencyUnit, Supplier<String> descriptionSupplier) {
        recorder.update(timestamp, latencyTime, latencyUnit, descriptionSupplier);
    }

    @Override
    synchronized public List<Position> getPositionsInDescendingOrder() {
        intervalRecorder = recorder.getIntervalRecorder(intervalRecorder);
        return intervalRecorder.getPositionsInDescendingOrder();
    }

    @Override
    public int getSize() {
        return intervalRecorder.getSize();
    }

}
