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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;


final class ResetByChunksCounter implements WindowCounter {

    // meaningful limits to disallow user to kill performance(or memory footprint) by mistake
    static final int MAX_CHUNKS = 100;
    static final long MIN_CHUNK_RESETTING_INTERVAL_MILLIS = 1000;

    private final boolean smoothlyEvictFromOldestChunk;
    private final long intervalBetweenResettingMillis;
    private final boolean reportUncompletedChunkToSnapshot;
    private final Clock clock;
    private final long creationTimestamp;
    private final LeftRightChunk[] chunks;

    ResetByChunksCounter(ChunkEvictionPolicy evictionPolicy, Clock clock) {
        this.smoothlyEvictFromOldestChunk = evictionPolicy.isSmoothlyEvictFromOldestChunk();

        this.intervalBetweenResettingMillis = evictionPolicy.getResettingPeriodMillis();
        if (intervalBetweenResettingMillis < MIN_CHUNK_RESETTING_INTERVAL_MILLIS) {
            throw new IllegalArgumentException("intervalBetweenResettingMillis should be >=" + MIN_CHUNK_RESETTING_INTERVAL_MILLIS);
        }

        this.reportUncompletedChunkToSnapshot = evictionPolicy.isReportUncompletedChunkToSnapshot();
        this.clock = clock;
        this.creationTimestamp = clock.getTime();

        if (evictionPolicy.getNumberChunks() > MAX_CHUNKS) {
            throw new IllegalArgumentException("number of chunks should be <=" + MAX_CHUNKS);
        }
        this.chunks = new LeftRightChunk[evictionPolicy.getNumberChunks()];
        for (int i = 0; i < chunks.length; i++) {
            this.chunks[i] = new LeftRightChunk(i);
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
    synchronized public long getSum() {
        long currentTimeMillis = clock.getTime();
        AtomicLong sum = new AtomicLong();
        boolean wasInterrupted = false;
        for (LeftRightChunk chunk : chunks) {
            wasInterrupted = wasInterrupted || chunk.addActivePhaseToSum(sum, currentTimeMillis);
        }
        if (wasInterrupted) {
            Thread.currentThread().interrupt();
        }
        return sum.get();
    }

    @Override
    synchronized public Long getValue() {
        return getSum();
    }

    private final class LeftRightChunk {

        final Phase left;
        final Phase right;
        final AtomicReference<Phase> currentPhaseRef;
        final AtomicInteger phaseMutators = new AtomicInteger(0);
        volatile Runnable postponedPhaseRotation = null;
        volatile Thread snapshotTakerThread = null;

        LeftRightChunk(int chunkIndex) {
            left = new Phase(creationTimestamp + (chunks.length + chunkIndex) * intervalBetweenResettingMillis);
            right = new Phase(Long.MAX_VALUE);
            this.currentPhaseRef = new AtomicReference<>(left);
        }

        boolean addActivePhaseToSum(AtomicLong sum, long currentTimeMillis) {
            Thread currentThread = Thread.currentThread();
            boolean wasInterrupted = false;

            // Save reference to current currentThread before increment of atomic,
            // it will provide guarantee that snapshot taker will be visible by writers
            this.snapshotTakerThread = currentThread;

            if (phaseMutators.incrementAndGet() > 1) {
                // phase rotation process is in progress by writer thread, it is need to park and wait permit from writer
                do {
                    LockSupport.park(this);
                    wasInterrupted = wasInterrupted || Thread.interrupted();
                    // Due to possibility of spurious wake up we need to wait in loop
                } while (phaseMutators.get() > 1);
            }
            try {
                Phase currentPhase = currentPhaseRef.get();
                currentPhase.addItselfToSum(sum, currentTimeMillis);
            } finally {
                if (phaseMutators.decrementAndGet() > 0) {
                    // the writer thread postponed rotation in order to provide for current thread ability to complete snapshot,
                    // so current thread need to complete rotation by itself
                    Runnable postponedPhaseRotation;
                    do {
                        LockSupport.park(this);
                        wasInterrupted = wasInterrupted || Thread.interrupted();
                    } while ((postponedPhaseRotation = this.postponedPhaseRotation) == null);
                    postponedPhaseRotation.run();
                }
            }
            this.snapshotTakerThread = null;
            return wasInterrupted;
        }

        void add(long delta, long currentTimeMillis) {
            Phase currentPhase = currentPhaseRef.get();
            if (currentTimeMillis < currentPhase.proposedInvalidationTimestamp) {
                currentPhase.recorder.add(delta);
                return;
            }

            Phase nextPhase = currentPhase == left ? right : left;
            if (!currentPhaseRef.compareAndSet(currentPhase, nextPhase)) {
                // another writer achieved progress and must clear current phase data, current writer tread just can write delta to next phase and return
                nextPhase.recorder.add(delta);
                return;
            }

            // Current thread is responsible to rotate phases.
            Runnable phaseRotation = () -> {
                try {
                    postponedPhaseRotation = null;
                    currentPhase.recorder.reset();
                    currentPhase.runningTotals.set(0);
                    currentPhase.proposedInvalidationTimestamp = Long.MAX_VALUE;
                    nextPhase.recorder.add(delta);
                } finally {
                    if (phaseMutators.decrementAndGet() > 0) {
                        // snapshot taker wait permit from current thread
                        LockSupport.unpark(this.snapshotTakerThread);
                    }
                    long millisSinceCreation = currentTimeMillis - creationTimestamp;
                    long intervalsSinceCreation = millisSinceCreation / intervalBetweenResettingMillis;
                    nextPhase.proposedInvalidationTimestamp = creationTimestamp + (intervalsSinceCreation + chunks.length) * intervalBetweenResettingMillis;
                }
            };

            // Need to be aware about snapshot takers in the middle of progress state
            if (phaseMutators.incrementAndGet() > 1) {
                // give chance to snapshot taker to finalize snapshot extraction, rotation will be complete by snapshot taker thread
                postponedPhaseRotation = phaseRotation;
                LockSupport.unpark(this.snapshotTakerThread);
            } else {
                // There are no active snapshot takers in the progress state, lets exchange phases in this writer thread
                phaseRotation.run();
            }
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("LeftRightChunk{");
            sb.append("currentPhase is ").append(currentPhaseRef.get() == left? "left" : "right");
            sb.append(", left=").append(left);
            sb.append(", right=").append(right);
            sb.append(", phaseMutators=").append(phaseMutators);
            sb.append(", postponedPhaseRotation=").append(postponedPhaseRotation);
            sb.append(", snapshotTakerThread=").append(snapshotTakerThread);
            sb.append('}');
            return sb.toString();
        }
    }

    private final class Phase {

        final AtomicLong runningTotals;
        final AtomicLongRecorder recorder;

        AtomicLong intervalAtomic;
        volatile long proposedInvalidationTimestamp;

        Phase(long proposedInvalidationTimestamp) {
            this.recorder = new AtomicLongRecorder();
            this.intervalAtomic = recorder.getIntervalAtomic();
            this.runningTotals = new AtomicLong();
            this.proposedInvalidationTimestamp = proposedInvalidationTimestamp;
        }

        void addItselfToSum(AtomicLong sum, long currentTimeMillis) {
            long proposedInvalidationTimestamp = this.proposedInvalidationTimestamp;
            if (currentTimeMillis >= proposedInvalidationTimestamp) {
                // The chunk was unused by writers for a long time
                return;
            }

            if (!reportUncompletedChunkToSnapshot) {
                if (currentTimeMillis < proposedInvalidationTimestamp - (chunks.length - 1) * intervalBetweenResettingMillis) {
                    // By configuration we should not add phase to snapshot until it fully completed
                    return;
                }
            }

            intervalAtomic = recorder.getIntervalAtomic(intervalAtomic);
            long runningTotalValue = runningTotals.addAndGet(intervalAtomic.get());
            if (smoothlyEvictFromOldestChunk) {
                long beforeInvalidateMillis = proposedInvalidationTimestamp - currentTimeMillis;
                if (beforeInvalidateMillis < intervalBetweenResettingMillis) {
                    runningTotalValue = (long) ((double)runningTotalValue * ((double)beforeInvalidateMillis / (double)intervalBetweenResettingMillis));
                }
            }
            sum.addAndGet(runningTotalValue);
        }

        @Override
        public String toString() {
            return "Phase{" +
                    "\n, proposedInvalidationTimestamp=" + proposedInvalidationTimestamp +
                    "\n, runningTotals=" + runningTotals +
                    "\n, intervalAtomic=" + intervalAtomic +
                    "\n}";
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
