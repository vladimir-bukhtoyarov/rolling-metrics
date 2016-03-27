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

package com.github.metricscore.hdrhistogram;

import com.codahale.metrics.Snapshot;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

class ResetPeriodicallyAccumulationStrategy implements AccumulationStrategy {

    private final long resetIntervalMillis;

    ResetPeriodicallyAccumulationStrategy(Duration resetPeriod) {
        if (resetPeriod.isNegative() || resetPeriod.isZero()) {
            throw new IllegalArgumentException("resetPeriod must be a positive duration");
        }
        this.resetIntervalMillis = resetPeriod.toMillis();
    }

    @Override
    public Accumulator createAccumulator(Recorder recorder, WallClock wallClock) {
        return new ResetPeriodicallyAccumulator(recorder, resetIntervalMillis, wallClock);
    }

    private static class ResetPeriodicallyAccumulator implements Accumulator {

        private static final long RESETTING_IN_PROGRESS_HAZARD = Long.MIN_VALUE;

        final Lock lock = new ReentrantLock();
        final Recorder recorder;
        final Histogram uniformHistogram;
        final long resetIntervalMillis;
        final WallClock wallClock;
        final AtomicLong nextResetTimeMillisRef;

        Histogram intervalHistogram;

        public ResetPeriodicallyAccumulator(Recorder recorder, long resetIntervalMillis, WallClock wallClock) {
            this.resetIntervalMillis = resetIntervalMillis;
            this.wallClock = wallClock;
            this.recorder = recorder;
            lock.lock();
            try {
                this.intervalHistogram = recorder.getIntervalHistogram();
            } finally {
                lock.unlock();
            }
            this.uniformHistogram = intervalHistogram.copy();
            nextResetTimeMillisRef = new AtomicLong(wallClock.currentTimeMillis() + resetIntervalMillis);
        }

        @Override
        public void recordValue(long value) {
            resetIfNeed();
            recorder.recordValue(value);
        }

        @Override
        public Snapshot getSnapshot(Function<Histogram, Snapshot> snapshotTaker) {
            lock.lock();
            try {
                resetIfNeed();
                intervalHistogram = recorder.getIntervalHistogram(intervalHistogram);
                uniformHistogram.add(intervalHistogram);
                return snapshotTaker.apply(uniformHistogram);
            } finally {
                lock.unlock();
            }
        }

        @Override
        public int getEstimatedFootprintInBytes() {
            return intervalHistogram.getEstimatedFootprintInBytes() * 3;
        }

        private void resetIfNeed() {
            long nextResetTimeMillis = nextResetTimeMillisRef.get();
            if (nextResetTimeMillis != RESETTING_IN_PROGRESS_HAZARD && wallClock.currentTimeMillis() >= nextResetTimeMillis) {
                if (!nextResetTimeMillisRef.compareAndSet(nextResetTimeMillis, RESETTING_IN_PROGRESS_HAZARD)) {
                    // another concurrent thread achieved progress and became responsible for resetting histograms
                    return;
                }
                // CAS was successful, so current thread became the responsible for resetting histograms
                lock.lock();
                try {
                    intervalHistogram = recorder.getIntervalHistogram(intervalHistogram);
                    uniformHistogram.reset();
                    nextResetTimeMillisRef.set(wallClock.currentTimeMillis() + resetIntervalMillis);
                } finally {
                    lock.unlock();
                }
            }
        }
    }

}
