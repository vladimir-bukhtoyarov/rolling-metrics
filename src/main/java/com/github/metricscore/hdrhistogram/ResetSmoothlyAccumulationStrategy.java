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

import com.codahale.metrics.Clock;
import com.codahale.metrics.Snapshot;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

class ResetSmoothlyAccumulationStrategy implements AccumulationStrategy {

    private final long measureTimeToLiveMillis;
    private final int numberChunks;
    private final Optional<ScheduledExecutorService> scheduler;

    ResetSmoothlyAccumulationStrategy(Duration resetPeriod, int numberChunks, Optional<ScheduledExecutorService> scheduler) {
        if (resetPeriod.isNegative() || resetPeriod.isZero()) {
            throw new IllegalArgumentException("resetPeriod must be a positive duration");
        }
        this.measureTimeToLiveMillis = resetPeriod.toMillis();
        this.numberChunks = numberChunks;
        this.scheduler = scheduler;
    }

    @Override
    public Accumulator createAccumulator(Recorder recorder, Clock clock) {
        if (scheduler.isPresent()) {
            SmoothlyResetByTimerAccumulator accumulator = new SmoothlyResetByTimerAccumulator(recorder);
            scheduler.get().scheduleAtFixedRate(accumulator::resetByTimer, measureTimeToLiveMillis, measureTimeToLiveMillis, TimeUnit.MILLISECONDS);
            return accumulator;
        } else {
            return new ResetSmoothlyAccumulator(recorder, measureTimeToLiveMillis, clock);
        }
    }

    private static class ResetSmoothlyAccumulator implements Accumulator {

        private static final long RESETTING_IN_PROGRESS_HAZARD = Long.MIN_VALUE;

        final Lock lock = new ReentrantLock();
        final Recorder recorder;
        final Histogram uniformHistogram;
        final long resetIntervalMillis;
        final Clock clock;
        final AtomicLong nextResetTimeMillisRef;

        Histogram intervalHistogram;

        ResetSmoothlyAccumulator(Recorder recorder, long resetIntervalMillis, Clock clock) {
            this.resetIntervalMillis = resetIntervalMillis;
            this.clock = clock;
            this.recorder = recorder;
            lock.lock();
            try {
                this.intervalHistogram = recorder.getIntervalHistogram();
            } finally {
                lock.unlock();
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
            if (nextResetTimeMillis != RESETTING_IN_PROGRESS_HAZARD && clock.getTime() >= nextResetTimeMillis) {
                if (!nextResetTimeMillisRef.compareAndSet(nextResetTimeMillis, RESETTING_IN_PROGRESS_HAZARD)) {
                    // another concurrent thread achieved progress and became responsible for resetting histograms
                    return;
                }
                // CAS was successful, so current thread became the responsible for resetting histograms
                lock.lock();
                try {
                    intervalHistogram = recorder.getIntervalHistogram(intervalHistogram);
                    uniformHistogram.reset();
                    nextResetTimeMillisRef.set(clock.getTime() + resetIntervalMillis);
                } finally {
                    lock.unlock();
                }
            }
        }
    }

    private static class SmoothlyResetByTimerAccumulator implements Accumulator {

        final Lock lock = new ReentrantLock();
        final Recorder recorder;
        final Histogram[] chunks;

        Histogram intervalHistogram;

        SmoothlyResetByTimerAccumulator(Recorder recorder, int numberChunks) {
            this.recorder = recorder;
            lock.lock();
            try {
                this.intervalHistogram = recorder.getIntervalHistogram();
            } finally {
                lock.unlock();
            }
            this.chunks = new Histogram[numberChunks];
            for (int i = 0; i < numberChunks; i++) {
                this.chunks[i] = intervalHistogram.copy();
            }
        }

        @Override
        public void recordSingleValueWithExpectedInterval(long value, long expectedIntervalBetweenValueSamples) {
            recorder.recordValueWithExpectedInterval(value, expectedIntervalBetweenValueSamples);
        }

        @Override
        public Snapshot getSnapshot(Function<Histogram, Snapshot> snapshotTaker) {
            lock.lock();
            try {
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

        void resetByTimer() {
            // CAS was successful, so current thread became the responsible for resetting histograms
            lock.lock();
            try {
                intervalHistogram = recorder.getIntervalHistogram(intervalHistogram);
                uniformHistogram.reset();
            } finally {
                lock.unlock();
            }
        }
    }

}
