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

package com.github.metricscore.hdrhistogram.accumulator;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Snapshot;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

public class ResetPeriodicallyAccumulator implements Accumulator {

    private static final long RESETTING_IN_PROGRESS_HAZARD = Long.MIN_VALUE;

    private final Recorder recorder;
    private final Histogram uniformHistogram;
    private final long resetIntervalMillis;
    private final Clock clock;
    private final AtomicLong nextResetTimeMillisRef;

    private Histogram intervalHistogram;

    public ResetPeriodicallyAccumulator(Recorder recorder, long resetIntervalMillis, Clock clock) {
        this.resetIntervalMillis = resetIntervalMillis;
        this.clock = clock;
        this.recorder = recorder;
        synchronized (this) {
            this.intervalHistogram = recorder.getIntervalHistogram();
        }
        this.uniformHistogram = intervalHistogram.copy();
        nextResetTimeMillisRef = new AtomicLong(clock.getTime() + resetIntervalMillis);
    }

    @Override
    public void recordSingleValueWithExpectedInterval(long value, long expectedIntervalBetweenValueSamples) {
        resetIfNeed();
        recorder.recordValueWithExpectedInterval(value, expectedIntervalBetweenValueSamples);
    }

    @Override
    public Snapshot getSnapshot(Function<Histogram, Snapshot> snapshotTaker) {
        synchronized (this) {
            resetIfNeed();
            intervalHistogram = recorder.getIntervalHistogram(intervalHistogram);
            uniformHistogram.add(intervalHistogram);
            return snapshotTaker.apply(uniformHistogram);
        }
    }

    @Override
    public int getEstimatedFootprintInBytes() {
        return intervalHistogram.getEstimatedFootprintInBytes() * 3;
    }

    private void resetIfNeed() {
        long nextResetTimeMillis = nextResetTimeMillisRef.get();
        if (nextResetTimeMillis != RESETTING_IN_PROGRESS_HAZARD && clock.getTime() >= nextResetTimeMillis) {
            if (!nextResetTimeMillisRef.compareAndSet(nextResetTimeMillis, RESETTING_IN_PROGRESS_HAZARD)) {
                // another concurrent thread achieved progress and became responsible for resetting histograms
                return;
            }
            // CAS was successful, so current thread became the responsible for resetting histograms
            synchronized (this) {
                intervalHistogram = recorder.getIntervalHistogram(intervalHistogram);
                uniformHistogram.reset();
                nextResetTimeMillisRef.set(clock.getTime() + resetIntervalMillis);
            }
        }
    }
}
