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
import com.github.metricscore.hdrhistogram.util.EmptySnapshot;
import com.github.metricscore.hdrhistogram.util.Printer;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Function;

public class ResetPeriodicallyAccumulator implements Accumulator {

    private static final long RESETTING_IN_PROGRESS_HAZARD = Long.MIN_VALUE;

    private final Recorder recorder;
    private final Histogram uniformHistogram;
    private final long resetIntervalMillis;
    private final Clock clock;
    private final AtomicLong nextResetTimeMillisRef;
    private final AtomicInteger activeMutators = new AtomicInteger(0);
    private volatile Runnable postponedRotation = null;

    private Histogram intervalHistogram;

    public ResetPeriodicallyAccumulator(Recorder recorder, long resetIntervalMillis, Clock clock) {
        this.resetIntervalMillis = resetIntervalMillis;
        this.clock = clock;
        this.recorder = recorder;
        synchronized (this) {
            this.intervalHistogram = recorder.getIntervalHistogram();
        }
        this.uniformHistogram = intervalHistogram.copy();
        this.nextResetTimeMillisRef = new AtomicLong(clock.getTime() + resetIntervalMillis);
    }

    @Override
    public void recordSingleValueWithExpectedInterval(long value, long expectedIntervalBetweenValueSamples) {
        long nextResetTimeMillis = nextResetTimeMillisRef.get();
        long currentTimeMillis = clock.getTime();
        if (nextResetTimeMillis != RESETTING_IN_PROGRESS_HAZARD && currentTimeMillis >= nextResetTimeMillis
                && nextResetTimeMillisRef.compareAndSet(nextResetTimeMillis, RESETTING_IN_PROGRESS_HAZARD)) {
            // Current thread is responsible to rotate phases.
            Runnable rotation = () -> {
                try {
                    postponedRotation = null;
                    recorder.reset();
                    uniformHistogram.reset();
                } finally {
                    activeMutators.decrementAndGet();
                    nextResetTimeMillisRef.set(currentTimeMillis + resetIntervalMillis);
                }
            };

            // Need to be aware about snapshot takers in the middle of progress state
            if (activeMutators.incrementAndGet() > 1) {
                // give chance to snapshot taker to finalize snapshot extraction, rotation will be completed by snapshot taker thread
                postponedRotation = rotation;
            } else {
                // There are no active snapshot takers in the progress state, lets exchange phases in this writer thread
                rotation.run();
            }
        }
        recorder.recordValueWithExpectedInterval(value, expectedIntervalBetweenValueSamples);
    }

    @Override
    public synchronized Snapshot getSnapshot(Function<Histogram, Snapshot> snapshotTaker) {
        while (!activeMutators.compareAndSet(0, 1)) {
            // if phase rotation process is in progress by writer thread then wait inside spin loop until rotation will done
            LockSupport.parkNanos(TimeUnit.MICROSECONDS.toNanos(500));
        }

        try {
            long currentTimeMillis = clock.getTime();
            long nextResetTimeMillis = nextResetTimeMillisRef.get();
            if (currentTimeMillis >= nextResetTimeMillis) {
                // pay nothing when reservoir is unused(by writers) for a long time
                return EmptySnapshot.INSTANCE;
            }
            intervalHistogram = recorder.getIntervalHistogram(intervalHistogram);
            uniformHistogram.add(intervalHistogram);
            return snapshotTaker.apply(uniformHistogram);
        } finally {
            if (activeMutators.decrementAndGet() > 0) {
                while (this.postponedRotation == null) {
                    // wait in spin loop until writer thread provide rotation task
                    LockSupport.parkNanos(TimeUnit.MICROSECONDS.toNanos(100));
                }
                postponedRotation.run();
            }
        }
    }

    @Override
    public int getEstimatedFootprintInBytes() {
        return intervalHistogram.getEstimatedFootprintInBytes() * 3;
    }

    @Override
    public String toString() {
        return "ResetPeriodicallyAccumulator{" +
                "\nuniformHistogram=" + Printer.histogramToString(uniformHistogram) +
                ",\n resetIntervalMillis=" + resetIntervalMillis +
                ",\n clock=" + clock +
                ",\n nextResetTimeMillisRef=" + nextResetTimeMillisRef +
                ",\n activeMutators=" + activeMutators.get() +
                ",\n intervalHistogram=" + Printer.histogramToString(intervalHistogram) +
                "\n}";
    }
}
