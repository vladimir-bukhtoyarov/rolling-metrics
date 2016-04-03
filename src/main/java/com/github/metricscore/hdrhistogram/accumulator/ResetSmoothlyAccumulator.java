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

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

public class ResetSmoothlyAccumulator implements Accumulator {

    final Lock lock = new ReentrantLock();
    final Recorder recorder;
    final Histogram[] chunks;
    final long intervalBetweenResetingChunksMillis;
    final Clock clock;

    volatile long nextResetTimeMillis;
    int snapshotIndex;
    Histogram intervalHistogram;

    public ResetSmoothlyAccumulator(Recorder recorder, int numberChunks, long measureTimeToLiveMillis, Clock clock) {
        this.intervalBetweenResetingChunksMillis = measureTimeToLiveMillis / numberChunks;
        this.clock = clock;
        this.recorder = recorder;
        lock.lock();
        try {
            this.intervalHistogram = recorder.getIntervalHistogram();
            this.chunks = new Histogram[numberChunks + 1];
            for (int i = 0; i < chunks.length; i++) {
                this.chunks[i] = intervalHistogram.copy();
            }

            long nowMillis = clock.getTime();
            for (int i = 1; i <= chunks.length; i++) {
                this.chunks[i].setEndTimeStamp(nowMillis + i * intervalBetweenResetingChunksMillis);
            }
            snapshotIndex = 0;
            nextResetTimeMillis = nowMillis + intervalBetweenResetingChunksMillis;
        } finally {
            lock.unlock();
        }
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

    private void resetIfNeed() {
        long nowMillis = clock.getTime();
        if (nowMillis < nextResetTimeMillis) {
            return;
        }

        lock.lock();
        try {
            nowMillis = clock.getTime();
            if (nowMillis < nextResetTimeMillis) {
                // histograms already cleared by another concurrent thread
                return;
            }

            intervalHistogram = recorder.getIntervalHistogram(intervalHistogram);

            long numberOfChuncksToReset = (nowMillis - nextResetTimeMillis) / intervalBetweenResetingChunksMillis + 1;
            if (numberOfChuncksToReset >= chunks.length) {
                // was unused for a long time, need to reset all chunks
                for (int i = 1; i <= chunks.length; i++) {
                    this.chunks[i -1].reset();
                    this.chunks[i].setEndTimeStamp(nowMillis + i * intervalBetweenResetingChunksMillis);
                }
                snapshotIndex = 0;
                nextResetTimeMillis = nowMillis + intervalBetweenResetingChunksMillis;
                return;
            }

            long proposedNextResetTimeMillis = Long.MAX_VALUE;
            for (int i = 1; i <= chunks.length; i++) {
                long chunkEndTimestamp = chunks[i].getEndTimeStamp();
                if (chunkEndTimestamp <= nowMillis) {
                    this.chunks[i].reset();
                    this.chunks[i].setEndTimeStamp(chunkEndTimestamp + chunks.length * intervalBetweenResetingChunksMillis);
                } else {
                    if (chunkEndTimestamp < proposedNextResetTimeMillis) {
                        proposedNextResetTimeMillis = chunkEndTimestamp;
                        snapshotIndex = i;
                    }
                }
            }
            nextResetTimeMillis = proposedNextResetTimeMillis;
        } finally {
            lock.unlock();
        }
    }
}
