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

package com.github.metricscore.hdr.histogram.accumulator;

import com.codahale.metrics.Snapshot;
import com.github.metricscore.hdr.histogram.util.Printer;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;

import java.util.function.Function;

public class ResetOnSnapshotAccumulator implements Accumulator {

    private final Recorder recorder;
    private Histogram intervalHistogram;

    public ResetOnSnapshotAccumulator(Recorder recorder) {
        this.recorder = recorder;
        synchronized (this) {
            this.intervalHistogram = recorder.getIntervalHistogram();
        }
    }

    @Override
    public void recordSingleValueWithExpectedInterval(long value, long expectedIntervalBetweenValueSamples) {
        recorder.recordValueWithExpectedInterval(value, expectedIntervalBetweenValueSamples);
    }

    @Override
    synchronized public final Snapshot getSnapshot(Function<Histogram, Snapshot> snapshotTaker) {
        intervalHistogram = recorder.getIntervalHistogram(intervalHistogram);
        return snapshotTaker.apply(intervalHistogram);
    }

    @Override
    public int getEstimatedFootprintInBytes() {
        return intervalHistogram.getEstimatedFootprintInBytes() * 2;
    }

    @Override
    public String toString() {
        return "ResetOnSnapshotAccumulator{" +
                "intervalHistogram=" + Printer.histogramToString(intervalHistogram) +
                '}';
    }
}
