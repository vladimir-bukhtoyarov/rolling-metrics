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

public class ResetByChunksAccumulator implements Accumulator {

    private final Recorder recorder;
    private final Chunk[] chunks;
    private final long chunkTimeToLiveMillis;
    private final Clock clock;

    private volatile long nextResetTimeMillis;
    private int snapshotIndex;
    private Histogram intervalHistogram;

    public ResetByChunksAccumulator(Recorder recorder, int numberChunks, long chunkTimeToLiveMillis, Clock clock) {
        this.chunkTimeToLiveMillis = chunkTimeToLiveMillis;
        this.clock = clock;
        this.recorder = recorder;
        synchronized (this) {
            long nowMillis = clock.getTime();
            this.intervalHistogram = recorder.getIntervalHistogram();
            this.chunks = new Chunk[numberChunks];
            for (int i = 0; i < chunks.length; i++) {
                this.chunks[i] = new Chunk(intervalHistogram.copy(), nowMillis + (i + 1) * this.chunkTimeToLiveMillis);
            }
            snapshotIndex = 0;
            nextResetTimeMillis = nowMillis + this.chunkTimeToLiveMillis;
        }
    }

    @Override
    public void recordSingleValueWithExpectedInterval(long value, long expectedIntervalBetweenValueSamples) {
        resetIfNeed();
        recorder.recordValueWithExpectedInterval(value, expectedIntervalBetweenValueSamples);
    }

    @Override
    public Snapshot getSnapshot(Function<Histogram, Snapshot> snapshotTaker) {
        synchronized (this) {
            if (!resetIfNeed()) {
                intervalHistogram = recorder.getIntervalHistogram(intervalHistogram);
                for (Chunk chunk : chunks) {
                    chunk.histogram.add(intervalHistogram);
                }
            }
            return snapshotTaker.apply(chunks[snapshotIndex].histogram);
        }
    }

    @Override
    public int getEstimatedFootprintInBytes() {
        return intervalHistogram.getEstimatedFootprintInBytes() * 3;
    }

    private boolean resetIfNeed() {
        long nowMillis = clock.getTime();
        if (nowMillis < nextResetTimeMillis) {
            return false;
        }

        synchronized (this) {
            nowMillis = clock.getTime();
            if (nowMillis < nextResetTimeMillis) {
                // histograms already cleared by another concurrent thread
                return false;
            }

            intervalHistogram = recorder.getIntervalHistogram(intervalHistogram);

            long numberOfChuncksToReset = (nowMillis - nextResetTimeMillis) / chunkTimeToLiveMillis + 1;
            if (numberOfChuncksToReset >= chunks.length) {
                // was unused for a long time, need to reset all chunks
                for (int i = 1; i <= chunks.length; i++) {
                    this.chunks[i -1].histogram.reset();
                    this.chunks[i].proposedEndTimestamp = nowMillis + i * chunkTimeToLiveMillis;
                }
                snapshotIndex = 0;
                nextResetTimeMillis = nowMillis + chunkTimeToLiveMillis;
                return true;
            }

            long proposedNextResetTimeMillis = Long.MAX_VALUE;
            for (int i = 1; i <= chunks.length; i++) {
                if (chunks[i].proposedEndTimestamp <= nowMillis) {
                    this.chunks[i].histogram.reset();
                    this.chunks[i].proposedEndTimestamp = chunks[i].proposedEndTimestamp + chunks.length * chunkTimeToLiveMillis;
                } else {
                    if (chunks[i].proposedEndTimestamp < proposedNextResetTimeMillis) {
                        proposedNextResetTimeMillis = chunks[i].proposedEndTimestamp;
                        snapshotIndex = i;
                        this.chunks[i].histogram.add(intervalHistogram);
                    }
                }
            }
            nextResetTimeMillis = proposedNextResetTimeMillis;
            return true;
        }
    }

    private static final class Chunk {

        final Histogram histogram;

        long proposedEndTimestamp;

        public Chunk(Histogram histogram, long proposedEndTimestamp) {
            this.proposedEndTimestamp = proposedEndTimestamp;
            this.histogram = histogram;
        }

    }


}
