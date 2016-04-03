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

import com.codahale.metrics.Snapshot;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

public class ResetSmoothlyByTimerAccumulator implements Accumulator {

    final Lock lock = new ReentrantLock();
    final Recorder recorder;
    final Histogram[] chunks;

    Histogram intervalHistogram;
    int snapshotIndex;

    public ResetSmoothlyByTimerAccumulator(Recorder recorder, Duration measureTimeToLive, int numberChunks, ScheduledExecutorService scheduler) {
        lock.lock();
        try {
            this.recorder = recorder;
            this.intervalHistogram = recorder.getIntervalHistogram();
            snapshotIndex = 0;
            this.chunks = new Histogram[numberChunks + 1];
            for (int i = 0; i < chunks.length; i++) {
                this.chunks[i] = intervalHistogram.copy();
            }
        } finally {
            lock.unlock();
        }
        long chunkTimeToLiveMillis = measureTimeToLive.toMillis() / numberChunks;
        scheduler.scheduleAtFixedRate(this::resetByTimer, chunkTimeToLiveMillis, chunkTimeToLiveMillis, TimeUnit.MILLISECONDS);
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
            for (int i = 0; i < chunks.length; i++) {
                chunks[i].add(intervalHistogram);
            }
            return snapshotTaker.apply(chunks[snapshotIndex]);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int getEstimatedFootprintInBytes() {
        return intervalHistogram.getEstimatedFootprintInBytes() * 3;
    }

    public void resetByTimer() {
        lock.lock();
        try {
            intervalHistogram = recorder.getIntervalHistogram(intervalHistogram);
            for (int i = 0; i < chunks.length; i++) {
                if (i != snapshotIndex) {
                    chunks[i].add(intervalHistogram);
                }
            }
            chunks[snapshotIndex].reset();
            snapshotIndex++;
            if (snapshotIndex == chunks.length) {
                snapshotIndex = 0;
            }
        } finally {
            lock.unlock();
        }
    }
}
