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

package com.github.rollingmetrics.histogram.accumulator;

import com.codahale.metrics.Snapshot;
import org.HdrHistogram.Histogram;

import java.util.function.Function;

/**
 * A responsible to updating and resetting {@link org.HdrHistogram.Recorder}
 *
 * This class is not the part of metrics-core-hdr public API and should not be used by user directly.
 */
public interface Accumulator {

    void recordSingleValueWithExpectedInterval(long value, long expectedIntervalBetweenValueSamples);

    Snapshot getSnapshot(Function<Histogram, Snapshot> snapshotTaker);

    int getEstimatedFootprintInBytes();

}
