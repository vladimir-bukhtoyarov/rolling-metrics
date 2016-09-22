/*
 *    Copyright 2016 Vladimir Bukhtoyarov
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

package com.github.metricscore.hdr.hitratio;

import com.codahale.metrics.Clock;
import com.github.metricscore.hdr.histogram.util.Printer;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by vladimir.bukhtoyarov on 22.09.2016.
 */
public class SmoothlyDecayingHitRatio implements HitRatio {

    // meaningful limits to disallow user to kill performance(or memory footprint) by mistake
    static final int MAX_CHUNKS = 100;
    static final long MIN_CHUNK_RESETTING_INTERVAL_MILLIS = 1000;

    private static final int HIT_INDEX = 0;
    private static final int TOTAL_INDEX = 0;

    private final long intervalBetweenResettingMillis;
    private final Clock clock;
    private final long creationTimestamp;

    private final Chunk[] chunks;

    /**
     * Constructs the chunked hit-ratio divided by {@code numberChunks}.
     * The hit-ratio will invalidate one chunk each time when {@code rollingWindow/numberChunks} millis has elapsed,
     * except oldest chunk which invalidated continuously.
     * The memory consumed by counter and latency of sum calculation depend directly from {@code numberChunks}
     *
     * <p> Example of usage:
     * <pre><code>
     *         // constructs the counter which divided by 10 chunks with 60 seconds time window.
     *         // one chunk will be reset to zero after each 6 second,
     *         SmoothlyDecayingHitRatio hitRatio = new SmoothlyDecayingHitRatio(Duration.ofSeconds(60), 10);
     *         counter.add(42);
     *     </code>
     * </pre>
     *
     * @param rollingWindow the rolling time window duration
     * @param numberChunks The count of chunk to split
     */
    public SmoothlyDecayingHitRatio(Duration rollingWindow, int numberChunks) {
        this(rollingWindow, numberChunks, Clock.defaultClock());
    }

    /**
     * @return the rolling window duration for this hit-ratio
     */
    public Duration getRollingWindow() {
        return Duration.ofMillis((chunks.length - 1) * intervalBetweenResettingMillis);
    }

    /**
     * @return the number of chunks
     */
    public int getChunkCount() {
        return chunks.length - 1;
    }

    SmoothlyDecayingHitRatio(Duration rollingWindow, int numberChunks, Clock clock) {
        if (numberChunks < 2) {
            throw new IllegalArgumentException("numberChunks should be >= 2");
        }

        if (numberChunks > MAX_CHUNKS) {
            throw new IllegalArgumentException("number of chunks should be <=" + MAX_CHUNKS);
        }

        long rollingWindowMillis = rollingWindow.toMillis();
        this.intervalBetweenResettingMillis = rollingWindowMillis / numberChunks;
        if (intervalBetweenResettingMillis < MIN_CHUNK_RESETTING_INTERVAL_MILLIS) {
            throw new IllegalArgumentException("intervalBetweenResettingMillis should be >=" + MIN_CHUNK_RESETTING_INTERVAL_MILLIS);
        }

        this.clock = clock;
        this.creationTimestamp = clock.getTime();

        this.chunks = new Chunk[numberChunks + 1];
        for (int i = 0; i < chunks.length; i++) {
            this.chunks[i] = new Chunk(i);
        }
    }

    @Override
    public void update(int hitCount, int totalCount) {
        long nowMillis = clock.getTime();
        long millisSinceCreation = nowMillis - creationTimestamp;
        long intervalsSinceCreation = millisSinceCreation / intervalBetweenResettingMillis;
        int chunkIndex = (int) intervalsSinceCreation % chunks.length;
        chunks[chunkIndex].update(hitCount, totalCount, nowMillis);
    }

    @Override
    public double getHitRatio() {
        long currentTimeMillis = clock.getTime();

        // To miss as less as possible we need to calculate sum in order from oldest to newest
        long millisSinceCreation = currentTimeMillis - creationTimestamp;
        long intervalsSinceCreation = millisSinceCreation / intervalBetweenResettingMillis;
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
            long invalidationTimestamp = creationTimestamp + (chunks.length + chunkIndex) * intervalBetweenResettingMillis;
            this.currentPhaseRef = new AtomicReference<>(new Phase(invalidationTimestamp));
        }

        void addToSnapshot(long[] snapshot, long currentTimeMillis) {
            currentPhaseRef.get().addToSnapshot(snapshot, currentTimeMillis);
        }

        void update(int hitCount, int totalCount, long currentTimeMillis) {
            Phase currentPhase = currentPhaseRef.get();
            while (currentTimeMillis >= currentPhase.proposedInvalidationTimestamp) {
                long millisSinceCreation = currentTimeMillis - creationTimestamp;
                long intervalsSinceCreation = millisSinceCreation / intervalBetweenResettingMillis;
                long nextProposedInvalidationTimestamp = creationTimestamp + (intervalsSinceCreation + chunks.length) * intervalBetweenResettingMillis;
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

            // if this is oldest chunk then we need to reduce its weight
            long beforeInvalidateMillis = proposedInvalidationTimestamp - currentTimeMillis;
            if (beforeInvalidateMillis < intervalBetweenResettingMillis) {
                double decayingCoefficient = (double) beforeInvalidateMillis / (double) intervalBetweenResettingMillis;
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
        return "SmoothlyDecayingRollingCounter{" +
                ", intervalBetweenResettingMillis=" + intervalBetweenResettingMillis +
                ", clock=" + clock +
                ", creationTimestamp=" + creationTimestamp +
                ", chunks=" + Printer.printArray(chunks, "chunk") +
                '}';
    }


}
