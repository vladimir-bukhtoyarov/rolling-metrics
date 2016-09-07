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

package com.github.metricscore.hdr.counter;

import com.codahale.metrics.Clock;
import com.github.metricscore.hdr.ChunkEvictionPolicy;
import com.github.metricscore.hdr.histogram.util.Printer;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;


final class ResetByChunksCounter implements WindowCounter {

    // meaningful limits to disallow user to kill performance(or memory footprint) by mistake
    static final int MAX_CHUNKS = 1000;
    static final long MIN_CHUNK_RESETTING_INTERVAL_MILLIS = 100;

    private final boolean smoothlyEvictFromOldestChunk;
    private final int numberChunks;
    private final long intervalBetweenResettingMillis;
    private final boolean reportUncompletedChunkToSnapshot;
    private final Clock clock;
    private final long creationTimestamp;

    private final Chunk[] chunks;

    ResetByChunksCounter(ChunkEvictionPolicy evictionPolicy, Clock clock) {
        this.smoothlyEvictFromOldestChunk = evictionPolicy.isSmoothlyEvictFromOldestChunk();

        this.intervalBetweenResettingMillis = evictionPolicy.getResettingPeriodMillis();
        if (intervalBetweenResettingMillis < MIN_CHUNK_RESETTING_INTERVAL_MILLIS) {
            throw new IllegalArgumentException("intervalBetweenResettingMillis should be >=" + MIN_CHUNK_RESETTING_INTERVAL_MILLIS);
        }

        this.reportUncompletedChunkToSnapshot = evictionPolicy.isReportUncompletedChunkToSnapshot();
        this.clock = clock;
        this.creationTimestamp = clock.getTime();

        this.numberChunks = evictionPolicy.getNumberChunks();
        if (numberChunks > MAX_CHUNKS) {
            throw new IllegalArgumentException("number of chunks should be <=" + MAX_CHUNKS);
        }
        this.chunks = new Chunk[evictionPolicy.getNumberChunks()];
        for (int i = 0; i < chunks.length; i++) {
            this.chunks[i] = new Chunk(i);
        }
    }

    @Override
    public void add(long delta) {
        if (delta < 1) {
            throw new IllegalArgumentException("value should be >= 1");
        }
        long nowMillis = clock.getTime();
        long millisSinceCreation = nowMillis - creationTimestamp;
        long intervalsSinceCreation = millisSinceCreation / intervalBetweenResettingMillis;
        int chunkIndex = (int) intervalsSinceCreation % chunks.length;
        chunks[chunkIndex].add(delta, nowMillis);
    }

    @Override
    public long getSum() {
        long currentTimeMillis = clock.getTime();
        long sum = 0;
        for (Chunk chunk : chunks) {
            sum += chunk.getSum(currentTimeMillis);
        }
        return sum;
    }

    @Override
    public Long getValue() {
        return getSum();
    }

    private final class Chunk {

        final AtomicReference<Phase> currentPhaseRef;

        Chunk(int chunkIndex) {
            long invalidationTimestamp = creationTimestamp + (chunks.length + chunkIndex) * intervalBetweenResettingMillis;
            this.currentPhaseRef = new AtomicReference<>(new Phase(invalidationTimestamp));
        }

        long getSum(long currentTimeMillis) {
            return currentPhaseRef.get().getSum(currentTimeMillis);
        }

        void add(long delta, long currentTimeMillis) {
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

            currentPhase.sum.addAndGet(delta);
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

        final AtomicLong sum;
        final long proposedInvalidationTimestamp;

        Phase(long proposedInvalidationTimestamp) {
            this.sum = new AtomicLong();
            this.proposedInvalidationTimestamp = proposedInvalidationTimestamp;
        }

        long getSum(long currentTimeMillis) {
            long proposedInvalidationTimestamp = this.proposedInvalidationTimestamp;
            if (currentTimeMillis >= proposedInvalidationTimestamp) {
                // The chunk was unused by writers for a long time
                return 0;
            }

            if (!reportUncompletedChunkToSnapshot) {
                if (currentTimeMillis < proposedInvalidationTimestamp - (chunks.length - 1) * intervalBetweenResettingMillis) {
                    // By configuration we should not add phase to snapshot until it fully completed
                    return 0;
                }
            }

            long sum = this.sum.get();
            if (smoothlyEvictFromOldestChunk) {
                long beforeInvalidateMillis = proposedInvalidationTimestamp - currentTimeMillis;
                if (beforeInvalidateMillis < intervalBetweenResettingMillis) {
                    sum = (long) ((double)sum * ((double)beforeInvalidateMillis / (double)intervalBetweenResettingMillis));
                }
            }
            return sum;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Phase{");
            sb.append("sum=").append(sum);
            sb.append(", proposedInvalidationTimestamp=").append(proposedInvalidationTimestamp);
            sb.append('}');
            return sb.toString();
        }
    }

    @Override
    public String toString() {
        return "ResetByChunksCounter{" +
                "smoothlyEvictFromOldestChunk=" + smoothlyEvictFromOldestChunk +
                ", intervalBetweenResettingMillis=" + intervalBetweenResettingMillis +
                ", reportUncompletedChunkToSnapshot=" + reportUncompletedChunkToSnapshot +
                ", clock=" + clock +
                ", creationTimestamp=" + creationTimestamp +
                ", chunks=" + Printer.printArray(chunks, "chunk") +
                '}';
    }

}
