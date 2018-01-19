/*
 *    Copyright 2017 Vladimir Bukhtoyarov
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package com.github.rollingmetrics.counter.impl;

import com.github.rollingmetrics.counter.WindowCounter;
import com.github.rollingmetrics.retention.ResetPeriodicallyByChunksRetentionPolicy;
import com.github.rollingmetrics.util.Ticker;
import com.github.rollingmetrics.util.Printer;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The rolling time window counter implementation which resets its state by chunks.
 */
class SmoothlyDecayingRollingCounter implements WindowCounter {

    // meaningful limits to disallow user to kill performance(or memory footprint) by mistake
    public static final int MAX_CHUNKS = 1000;
    public static final long MIN_CHUNK_RESETTING_INTERVAL_MILLIS = 100;

    private final long intervalBetweenResettingOneChunkMillis;
    private final Ticker ticker;
    private final long creationTime;

    private final Chunk[] chunks;


    SmoothlyDecayingRollingCounter(ResetPeriodicallyByChunksRetentionPolicy retentionPolicy, Ticker ticker) {
        int numberChunks = retentionPolicy.getNumberChunks();
        if (numberChunks > SmoothlyDecayingRollingCounter.MAX_CHUNKS) {
            throw new IllegalArgumentException("number of chunks should be <=" + SmoothlyDecayingRollingCounter.MAX_CHUNKS);
        }
        if (retentionPolicy.getIntervalBetweenResettingOneChunkMillis() < SmoothlyDecayingRollingCounter.MIN_CHUNK_RESETTING_INTERVAL_MILLIS) {
            throw new IllegalArgumentException("intervalBetweenResettingMillis should be >=" + SmoothlyDecayingRollingCounter.MIN_CHUNK_RESETTING_INTERVAL_MILLIS);
        }

        long rollingWindowMillis = retentionPolicy.getRollingTimeWindow().toMillis();
        this.intervalBetweenResettingOneChunkMillis = rollingWindowMillis / numberChunks;

        this.ticker = ticker;
        this.creationTime = ticker.stableMilliseconds();

        this.chunks = new Chunk[numberChunks + 1];
        for (int i = 0; i < chunks.length; i++) {
            this.chunks[i] = new Chunk(i);
        }
    }

    @Override
    public void add(long delta) {
        long nowMillis = ticker.stableMilliseconds();
        long millisSinceCreation = nowMillis - creationTime;
        long intervalsSinceCreation = millisSinceCreation / intervalBetweenResettingOneChunkMillis;
        int chunkIndex = (int) intervalsSinceCreation % chunks.length;
        chunks[chunkIndex].add(delta, nowMillis);
    }

    @Override
    public long getSum() {
        long currentTimeMillis = ticker.stableMilliseconds();

        // To get as fresh value as possible we need to calculate sum in order from oldest to newest
        long millisSinceCreation = currentTimeMillis - creationTime;
        long intervalsSinceCreation = millisSinceCreation / intervalBetweenResettingOneChunkMillis;
        int newestChunkIndex = (int) intervalsSinceCreation % chunks.length;

        long sum = 0;
        for (int i = newestChunkIndex + 1, iteration = 0; iteration < chunks.length; i++, iteration++) {
            if (i == chunks.length) {
                i = 0;
            }
            Chunk chunk = chunks[i];
            sum += chunk.getSum(currentTimeMillis);
        }
        return sum;
    }

    private final class Chunk {

        final AtomicReference<Phase> currentPhaseRef;

        Chunk(int chunkIndex) {
            long invalidationTime = creationTime + (chunks.length + chunkIndex) * intervalBetweenResettingOneChunkMillis;
            this.currentPhaseRef = new AtomicReference<>(new Phase(invalidationTime));
        }

        long getSum(long currentTimeMillis) {
            return currentPhaseRef.get().getSum(currentTimeMillis);
        }

        void add(long delta, long currentTimeMillis) {
            Phase currentPhase = currentPhaseRef.get();
            while (currentTimeMillis >= currentPhase.proposedInvalidationTime) {
                long millisSinceCreation = currentTimeMillis - creationTime;
                long intervalsSinceCreation = millisSinceCreation / intervalBetweenResettingOneChunkMillis;
                long nextProposedInvalidationTime = creationTime + (intervalsSinceCreation + chunks.length) * intervalBetweenResettingOneChunkMillis;
                Phase replacement = new Phase(nextProposedInvalidationTime);
                if (currentPhaseRef.compareAndSet(currentPhase, replacement)) {
                    currentPhase = replacement;
                } else {
                    currentPhase = currentPhaseRef.get();
                }
            }

            currentPhase.sum.addAndGet(delta);
        }

        @Override
        public String toString() {
            return "Chunk{" + "currentPhaseRef=" + currentPhaseRef +
                    '}';
        }
    }

    private final class Phase {

        final AtomicLong sum;
        final long proposedInvalidationTime;

        Phase(long proposedInvalidationTime) {
            this.sum = new AtomicLong();
            this.proposedInvalidationTime = proposedInvalidationTime;
        }

        long getSum(long currentTimeMillis) {
            long proposedInvalidationTime = this.proposedInvalidationTime;
            if (currentTimeMillis >= proposedInvalidationTime) {
                // The chunk was unused by writers for a long time
                return 0;
            }

            long sum = this.sum.get();

            // if this is oldest chunk then we need to reduce its weight
            long beforeInvalidateMillis = proposedInvalidationTime - currentTimeMillis;
            if (beforeInvalidateMillis < intervalBetweenResettingOneChunkMillis) {
                double decayingCoefficient = (double) beforeInvalidateMillis / (double) intervalBetweenResettingOneChunkMillis;
                sum = (long) ((double) sum * decayingCoefficient);
            }

            return sum;
        }

        @Override
        public String toString() {
            return "Phase{" + "sum=" + sum +
                    ", proposedInvalidationTimestamp=" + proposedInvalidationTime +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "SmoothlyDecayingRollingCounter{" +
                ", intervalBetweenResettingMillis=" + intervalBetweenResettingOneChunkMillis +
                ", ticker=" + ticker +
                ", creationTimestamp=" + creationTime +
                ", chunks=" + Printer.printArray(chunks, "chunk") +
                '}';
    }

}
