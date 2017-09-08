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

package com.github.rollingmetrics.histogram.hdr.impl;

import com.github.rollingmetrics.histogram.hdr.HdrHistogramUtil;
import com.github.rollingmetrics.histogram.hdr.RecorderSettings;
import com.github.rollingmetrics.histogram.hdr.RollingSnapshot;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;

import java.util.function.Function;

public class UniformRollingHdrHistogramImpl extends AbstractRollingHdrHistogram {

    private final Recorder recorder;
    private final Histogram uniformHistogram;

    private Histogram intervalHistogram;

    public UniformRollingHdrHistogramImpl(RecorderSettings recorderSettings) {
        super(recorderSettings);
        this.recorder = recorderSettings.buildRecorder();
        this.intervalHistogram = recorder.getIntervalHistogram();
        this.uniformHistogram = HdrHistogramUtil.createNonConcurrentCopy(intervalHistogram);
    }

    @Override
    public void recordSingleValueWithExpectedInterval(long value, long expectedIntervalBetweenValueSamples) {
        recorder.recordValueWithExpectedInterval(value, expectedIntervalBetweenValueSamples);
    }

    @Override
    public final synchronized RollingSnapshot getSnapshot(Function<Histogram, RollingSnapshot> snapshotTaker) {
        intervalHistogram = recorder.getIntervalHistogram(intervalHistogram);
        HdrHistogramUtil.addSecondToFirst(uniformHistogram, intervalHistogram);
        return HdrHistogramUtil.getSnapshot(uniformHistogram, snapshotTaker);
    }

    @Override
    public int getEstimatedFootprintInBytes() {
        return intervalHistogram.getEstimatedFootprintInBytes() * 3;
    }

    @Override
    public String toString() {
        return "UniformRollingHdrHistogramImpl{" +
            "\nuniformHistogram=" + histogramValuesToString(uniformHistogram) +
            "\n, intervalHistogram=" + histogramValuesToString(intervalHistogram) +
            '}';
    }

}
