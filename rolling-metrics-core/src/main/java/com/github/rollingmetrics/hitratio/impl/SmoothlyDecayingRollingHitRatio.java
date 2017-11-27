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

package com.github.rollingmetrics.hitratio.impl;

import com.github.rollingmetrics.hitratio.HitRatio;
import com.github.rollingmetrics.retention.ResetPeriodicallyByChunksRetentionPolicy;
import com.github.rollingmetrics.util.Ticker;
import com.github.rollingmetrics.util.Printer;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The rolling time window hit-ratio implementation which resets its state by chunks.
 *
 * The unique properties which makes this hit-ratio probably the best "rolling time window" implementation are following:
 * <ul>
 *     <li>Sufficient performance about tens of millions concurrent writes and reads per second.</li>
 *     <li>Predictable and low memory consumption, the memory which consumed by hit-ratio does not depend from amount and frequency of writes.</li>
 *     <li>Perfectly user experience, the continuous observation does not see the sudden changes of sum.
 *     This property achieved by smoothly decaying of oldest chunk of hit-ratio.
 *     </li>
 * </ul>
 *
 * <p>
 * Concurrency properties:
 * <ul>
 *     <li>Writing is lock-free.
 *     <li>Ratio calculation is lock-free.
 * </ul>
 *
 * <p>
 * Usage recommendations:
 * <ul>
 *     <li>Only when you need in "rolling time window" semantic.</li>
 * </ul>
 *
 * <p>
 * Performance considerations:
 * <ul>
 *     <li>You can consider writing speed as a constant. The write latency does not depend from count of chunk or frequency of chunk rotation.
 *     <li>The writing depends only from level of contention between writers(internally hit-ratio implemented across AtomicLong).</li>
 *     <li>The huge count of chunk leads to the slower calculation of their ratio. So precision of getHitRatio conflicts with latency of getHitRatio. You need to choose meaningful values.
 *     For example 10 chunks will guarantee at least 90% accuracy and ten million reads per second.</li>
 * </ul>
 *
 * @see ResetOnSnapshotHitRatio
 * @see ResetPeriodicallyHitRatio
 * @see UniformHitRatio
 */
class SmoothlyDecayingRollingHitRatio implements HitRatio {

    // meaningful limits to disallow user to kill performance(or memory footprint) by mistake
    public static final int MAX_CHUNKS = 100;
    public static final long MIN_CHUNK_RESETTING_INTERVAL_MILLIS = 1000;

    private static final int HIT_INDEX = 0;
    private static final int TOTAL_INDEX = 1;

    private final long intervalBetweenResettingOneChunkMillis;
    private final Ticker ticker;
    private final long creationTimestamp;

    private final Chunk[] chunks;

    /**
     * Constructs the chunked hit-ratio divided by {@code numberChunks}.
     * The hit-ratio will invalidate one chunk each time when {@code rollingWindow/numberChunks} millis has elapsed,
     * except oldest chunk which invalidated continuously.
     * The memory consumed by hit-ratio and latency of ratio calculation depend directly from {@code numberChunks}
     *
     * @param retentionPolicy the rolling time window duration TODO The count of chunk to split
     * @param ticker
     */
    SmoothlyDecayingRollingHitRatio(ResetPeriodicallyByChunksRetentionPolicy retentionPolicy, Ticker ticker) {
        int numberChunks = retentionPolicy.getNumberChunks();
        this.intervalBetweenResettingOneChunkMillis = retentionPolicy.getIntervalBetweenResettingOneChunkMillis();
        if (numberChunks > SmoothlyDecayingRollingHitRatio.MAX_CHUNKS) {
            throw new IllegalArgumentException("number of chunks should be <=" + SmoothlyDecayingRollingHitRatio.MAX_CHUNKS);
        }
        if (intervalBetweenResettingOneChunkMillis < SmoothlyDecayingRollingHitRatio.MIN_CHUNK_RESETTING_INTERVAL_MILLIS) {
            throw new IllegalArgumentException("rollingWindowMillis should be >=" + SmoothlyDecayingRollingHitRatio.MIN_CHUNK_RESETTING_INTERVAL_MILLIS);
        }

        this.ticker = ticker;
        this.creationTimestamp = ticker.stableMilliseconds();

        this.chunks = new Chunk[numberChunks + 1];
        for (int i = 0; i < chunks.length; i++) {
            this.chunks[i] = new Chunk(i);
        }
    }

    @Override
    public void update(int hitCount, int totalCount) {
        long nowMillis = ticker.stableMilliseconds();
        long millisSinceCreation = nowMillis - creationTimestamp;
        long intervalsSinceCreation = millisSinceCreation / intervalBetweenResettingOneChunkMillis;
        int chunkIndex = (int) intervalsSinceCreation % chunks.length;
        chunks[chunkIndex].update(hitCount, totalCount, nowMillis);
    }

    @Override
    public double getHitRatio() {
        long currentTimeMillis = ticker.stableMilliseconds();

        // To get as fresh value as possible we need to calculate ratio in order from oldest to newest
        long millisSinceCreation = currentTimeMillis - creationTimestamp;
        long intervalsSinceCreation = millisSinceCreation / intervalBetweenResettingOneChunkMillis;
        int newestChunkIndex = (int) intervalsSinceCreation % chunks.length;

        long[] snapshot = new long[2];
        for (int i = newestChunkIndex + 1, iteration = 0; iteration < chunks.length; i++, iteration++) {
            if (i == chunks.length) {
                i = 0;
            }
            Chunk chunk = chunks[i];
            chunk.addToSnapshot(snapshot, currentTimeMillis);
        }
        return (double) snapshot[HIT_INDEX] / (double) snapshot[TOTAL_INDEX];
    }

    private final class Chunk {

        final AtomicReference<Phase> currentPhaseRef;

        Chunk(int chunkIndex) {
            long invalidationTimestamp = creationTimestamp + (chunks.length + chunkIndex) * intervalBetweenResettingOneChunkMillis;
            this.currentPhaseRef = new AtomicReference<>(new Phase(invalidationTimestamp));
        }

        void addToSnapshot(long[] snapshot, long currentTimeMillis) {
            currentPhaseRef.get().addToSnapshot(snapshot, currentTimeMillis);
        }

        void update(int hitCount, int totalCount, long currentTimeMillis) {
            Phase currentPhase = currentPhaseRef.get();
            while (currentTimeMillis >= currentPhase.proposedInvalidationTimestamp) {
                long millisSinceCreation = currentTimeMillis - creationTimestamp;
                long intervalsSinceCreation = millisSinceCreation / intervalBetweenResettingOneChunkMillis;
                long nextProposedInvalidationTimestamp = creationTimestamp + (intervalsSinceCreation + chunks.length) * intervalBetweenResettingOneChunkMillis;
                Phase replacement = new Phase(nextProposedInvalidationTimestamp);
                if (currentPhaseRef.compareAndSet(currentPhase, replacement)) {
                    currentPhase = replacement;
                } else {
                    currentPhase = currentPhaseRef.get();
                }
            }
            HitRatioUtil.updateRatio(currentPhase.ratio, hitCount, totalCount);
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Chunk{");
            sb.append("currentPhaseRef=").append(currentPhaseRef);
            sb.append('}');
            return sb.toString();
        }
    }

    private final class Phase {

        final AtomicLong ratio;
        final long proposedInvalidationTimestamp;

        Phase(long proposedInvalidationTimestamp) {
            this.ratio = new AtomicLong();
            this.proposedInvalidationTimestamp = proposedInvalidationTimestamp;
        }

        void addToSnapshot(long[] snapshot, long currentTimeMillis) {
            long proposedInvalidationTimestamp = this.proposedInvalidationTimestamp;
            if (currentTimeMillis >= proposedInvalidationTimestamp) {
                // The chunk was unused by writers for a long time
                return;
            }

            long compositeRatio = ratio.get();
            int hitCount = HitRatioUtil.getHitFromCompositeRatio(compositeRatio);
            int totalCount = HitRatioUtil.getTotalCountFromCompositeRatio(compositeRatio);
            if (totalCount == 0) {
                return;
            }

            // if this is oldest chunk then we need to reduce its weight
            long beforeInvalidateMillis = proposedInvalidationTimestamp - currentTimeMillis;
            if (beforeInvalidateMillis < intervalBetweenResettingOneChunkMillis) {
                double decayingCoefficient = (double) beforeInvalidateMillis / (double) intervalBetweenResettingOneChunkMillis;
                hitCount = (int) (hitCount * decayingCoefficient);
                totalCount = (int) (totalCount * decayingCoefficient);
            }

            snapshot[HIT_INDEX] += hitCount;
            snapshot[TOTAL_INDEX] += totalCount;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Phase{");
            sb.append("ratio=").append(ratio);
            sb.append(", proposedInvalidationTimestamp=").append(proposedInvalidationTimestamp);
            sb.append('}');
            return sb.toString();
        }
    }

    @Override
    public String toString() {
        return "SmoothlyDecayingRollingHitRatio{" +
                ", intervalBetweenResettingMillis=" + intervalBetweenResettingOneChunkMillis +
                ", ticker=" + ticker +
                ", creationTimestamp=" + creationTimestamp +
                ", chunks=" + Printer.printArray(chunks, "chunk") +
                '}';
    }


}
