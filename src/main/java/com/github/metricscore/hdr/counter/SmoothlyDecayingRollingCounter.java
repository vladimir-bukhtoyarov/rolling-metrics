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
import com.github.metricscore.hdr.histogram.util.Printer;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The counter which resets its state by chunks, the count of chunk and interval between chunk resetting(except oldest chunk) are configurable at construction and can not be changed after.
 *
 * The unique properties which makes this counter probably the best "rolling time window" implementation are following:
 * <ul>
 *     <li>Sufficient performance about tens of millions concurrent writes and reads per second.</li>
 *     <li>Predictable and low memory consumption, the memory which consumed by counter does not depend from amount and frequency of writes.</li>
 *     <li>Perfectly user experience, the continuous observation does not see the sudden changes of sum.
 *     This property achieved by smoothly decaying of oldest chunk of counter. For the proof run <a href="https://github.com/vladimir-bukhtoyarov/metrics-core-hdr/blob/1.3/src/test/java/examples/SmoothlyDecayingRollingCounterPrecisionDemo.java">SmoothlyDecayingRollingCounterPrecisionDemo</a>.
 *     </li>
 * </ul>
 *
 * <p>
 * Concurrency properties:
 * <ul>
 *     <li>Writing is lock-free.
 *     <li>Sum reading is lock-free.
 * </ul>
 * </p>
 *
 * <p>
 * Usage recommendations:
 * <ul>
 *     <li>Only when you need in "rolling time window" semantic.
 *     <li>Only if you accept the fact that several increments can be reported to reader twice(because the frequency of reading exceeds the rate of decay of the counter.).</li>
 * </ul>
 * </p>
 *
 * <p>
 * Performance considerations:
 * <ul>
 *     <li>You can consider writing speed as a constant. The write latency does not depend from count of chunk or frequency of chunk rotation.
 *     <li>The writing depends only from level of contention between writers(internally counter implemented across AtomicLong).</li>
 *     <li>The huge count of chunk leads to the slower calculation of their sum. So precision of sum conflicts with latency of sum. You need to choose meaningful values.
 *     For example 10 chunks will guarantee at least 90% accuracy and ten million reads per second.</li>
 * </ul>
 * </p>
 *
 * <p> Example of usage:
 * <pre><code>
 *         // constructs the counter which divided by 10 chunks and one chunk will be reset to zero after each 6 second,
 *         // so counter will hold values written at last 60 seconds (10 * 6).
 *         WindowCounter counter = new SmoothlyDecayingRollingCounter(Duration.ofSeconds(6), 10);
 *         counter.add(42);
 *     </code>
 * </pre>
 * </p>
 */
public class SmoothlyDecayingRollingCounter implements WindowCounter {

    // meaningful limits to disallow user to kill performance(or memory footprint) by mistake
    static final int MAX_CHUNKS = 1000;
    static final long MIN_CHUNK_RESETTING_INTERVAL_MILLIS = 100;

    private final long intervalBetweenResettingMillis;
    private final Clock clock;
    private final long creationTimestamp;

    private final Chunk[] chunks;

    /**
     * Constructs the chunked counter which invalidate one chunk each time when {@intervalBetweenChunkResetting} has elapsed(except oldest chunk which invalidated continuously).
     * The memory consumed by counter and latency of sum calculation depend directly from {@code numberChunks}
     *
     * <p> Example of usage:
     * <pre><code>
     *         // constructs the counter which divided by 10 chunks and one chunk will be reset to zero after each 6 second,
     *         // so counter will hold values written at last 60 seconds (10 * 6).
     *         WindowCounter counter = new SmoothlyDecayingRollingCounter(Duration.ofSeconds(6), 10);
     *         counter.add(42);
     *     </code>
     * </pre>
     * </p>
     *
     * @param intervalBetweenChunkResetting the interval between chunk resetting
     * @param numberChunks The count of chunk to split counter
     */
    public SmoothlyDecayingRollingCounter(Duration intervalBetweenChunkResetting, int numberChunks) {
        this(intervalBetweenChunkResetting, numberChunks, Clock.defaultClock());
    }

    SmoothlyDecayingRollingCounter(Duration intervalBetweenChunkResetting, int numberChunks, Clock clock) {
        if (numberChunks < 2) {
            throw new IllegalArgumentException("numberChunks should be >= 2");
        }

        this.intervalBetweenResettingMillis = intervalBetweenChunkResetting.toMillis();
        if (intervalBetweenResettingMillis < MIN_CHUNK_RESETTING_INTERVAL_MILLIS) {
            throw new IllegalArgumentException("intervalBetweenResettingMillis should be >=" + MIN_CHUNK_RESETTING_INTERVAL_MILLIS);
        }

        this.clock = clock;
        this.creationTimestamp = clock.getTime();

        if (numberChunks > MAX_CHUNKS) {
            throw new IllegalArgumentException("number of chunks should be <=" + MAX_CHUNKS);
        }

        this.chunks = new Chunk[numberChunks + 1];
        for (int i = 0; i < chunks.length; i++) {
            this.chunks[i] = new Chunk(i);
        }
    }

    @Override
    public void add(long delta) {
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

            long sum = this.sum.get();

            // if this is oldest chunk then we need to
            long beforeInvalidateMillis = proposedInvalidationTimestamp - currentTimeMillis;
            if (beforeInvalidateMillis < intervalBetweenResettingMillis) {
                sum = (long) ((double)sum * ((double)beforeInvalidateMillis / (double)intervalBetweenResettingMillis));
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
        return "SmoothlyDecayingRollingCounter{" +
                ", intervalBetweenResettingMillis=" + intervalBetweenResettingMillis +
                ", clock=" + clock +
                ", creationTimestamp=" + creationTimestamp +
                ", chunks=" + Printer.printArray(chunks, "chunk") +
                '}';
    }

}
