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

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;


public class ResetByChunksCounter implements WindowCounter {

    private static final long NEUTRAL_VALUE = 0L;

    private final ChunkEvictionPolicy evictionPolicy;
    private final long creationTimestamp;
    private final long intervalBetweenResettingMillis;
    private final Chunk[] chunks;
    private final Clock clock;

    private final Phase left;
    private final Phase right;
    private final Phase[] phases;
    private final AtomicReference<Phase> currentPhaseRef;
    private final AtomicInteger phaseMutators = new AtomicInteger(0);

    private volatile Runnable rotationPostponedByWriter = null;
    private volatile Thread snapshotTakerThread = null;

    ResetByChunksCounter(ChunkEvictionPolicy evictionPolicy, Clock clock) {
        this.evictionPolicy = evictionPolicy;
        this.clock = clock;
        this.creationTimestamp = clock.getTime();
        this.intervalBetweenResettingMillis = evictionPolicy.getResettingPeriodMillis();

        this.left = new Phase(creationTimestamp +  intervalBetweenResettingMillis);
        this.right = new Phase(Long.MAX_VALUE);
        this.phases = new Phase[] {left, right};
        this.currentPhaseRef = new AtomicReference<>(left);

        this.chunks = new Chunk[evictionPolicy.getNumberChunks()];
        for (int i = 0; i < chunks.length; i++) {
            this.chunks[i] = new Chunk(Long.MIN_VALUE);
        }
    }

    @Override
    public void add(long delta) {
        if (delta < 1) {
            throw new IllegalArgumentException("value should be >= 1");
        }
        long currentTimeMillis = clock.getTime();
        recordOrTouch(delta, currentTimeMillis, true);
    }

    @Override
    synchronized public long getSum() {
        long sum = 0;

        Thread currentThread = Thread.currentThread();
        boolean wasInterrupted = false;

        // Save reference to current currentThread before increment of atomic,
        // it will provide guarantee that snapshot taker will be visible by writers
        this.snapshotTakerThread = currentThread;

        if (phaseMutators.incrementAndGet() > 1) {
            // phase rotation process is in progress by writer thread, it is need to park and wait permit from writer
            do {
                LockSupport.park();
                wasInterrupted = wasInterrupted || Thread.interrupted();
                // Due to possibility of spurious wake up we need to wait in loop
            } while (phaseMutators.get() > 1);
        }

        long currentTimeMillis = clock.getTime();

        try {
            for (Phase phase : phases) {
                if (phase.isNeedToBeReportedToSnapshot(currentTimeMillis)) {
                    sum += phase.getSum(currentTimeMillis);
                }
            }
            for (Chunk chunk : chunks) {
                if (chunk.proposedInvalidationTimestamp > currentTimeMillis) {
                    sum += chunk.getSum(currentTimeMillis);
                }
            }
        } finally {
            if (phaseMutators.decrementAndGet() > 0) {
                Runnable postponedPhaseRotation = this.rotationPostponedByWriter;
                if (postponedPhaseRotation != null) {
                    postponedPhaseRotation.run();
                }
                while (this.rotationPostponedByWriter == null) {
                    // wait in spin loop until writer currentThread provide rotation task
                    LockSupport.parkNanos(TimeUnit.MICROSECONDS.toNanos(100));
                }
                this.snapshotTakerThread = null;
                postponedPhaseRotation.run();
            } else {
                this.snapshotTakerThread = null;
            }
        }
        if (wasInterrupted) {
            currentThread.interrupt();
        }
        return sum;
    }

    @Override
    synchronized public Long getValue() {
        return getSum();
    }

    private void recordOrTouch(long delta, long currentTimeMillis, boolean currentThreadIsWriter) {
        Phase currentPhase = currentPhaseRef.get();
        if (currentTimeMillis < currentPhase.proposedInvalidationTimestamp) {
            if (currentThreadIsWriter) {
                currentPhase.sum.addAndGet(delta);
            }
            return;
        }

        Phase nextPhase = currentPhase == left ? right : left;
        if (currentThreadIsWriter) {
            nextPhase.sum.addAndGet(delta);
        }

        if (!currentPhaseRef.compareAndSet(currentPhase, nextPhase)) {
            // another writer achieved progress and must clear current phase data, current writer tread just can write delta to next phase and return
            return;
        }

        // Current thread is responsible to rotate phases.
        Runnable phaseRotation = () -> {
            try {
                rotationPostponedByWriter = null;

                // move values from recorder to correspondent chunk
                long currentPhaseNumber = (currentPhase.proposedInvalidationTimestamp - creationTimestamp) / intervalBetweenResettingMillis;
                int correspondentChunkIndex = (int) (currentPhaseNumber - 1) % chunks.length;
                Chunk correspondentChunk = chunks[correspondentChunkIndex];
                correspondentChunk.sum.set(currentPhase.sum.get());
                correspondentChunk.proposedInvalidationTimestamp = currentPhase.proposedInvalidationTimestamp + (chunks.length - 1) * intervalBetweenResettingMillis;
            } finally {
                long millisSinceCreation = currentTimeMillis - creationTimestamp;
                long intervalsSinceCreation = millisSinceCreation / intervalBetweenResettingMillis;
                currentPhase.proposedInvalidationTimestamp = Long.MAX_VALUE;

                if (phaseMutators.decrementAndGet() > 0) {
                    // snapshot taker wait permit from current thread
                    LockSupport.unpark(this.snapshotTakerThread);
                }
                nextPhase.proposedInvalidationTimestamp = creationTimestamp + (intervalsSinceCreation + 1) * intervalBetweenResettingMillis;
            }
        };

        // Need to be aware about snapshot takers in the middle of progress state
        if (phaseMutators.incrementAndGet() > 1 && currentThreadIsWriter) {
            // give chance to snapshot taker to finalize snapshot extraction, rotation will be complete by snapshot taker thread
            rotationPostponedByWriter = phaseRotation;
            LockSupport.unpark(snapshotTakerThread);
        } else {
            // There are no active snapshot takers in the progress state, lets exchange phases in this writer thread
            phaseRotation.run();
        }
    }

    private final class Chunk {

        private final AtomicLong sum;
        private volatile long proposedInvalidationTimestamp;

        public Chunk(long proposedInvalidationTimestamp) {
            this.proposedInvalidationTimestamp = proposedInvalidationTimestamp;
            this.sum = new AtomicLong();
        }

        @Override
        public String toString() {
            return "Chunk{" +
                    "sum=" + sum +
                    ", proposedInvalidationTimestamp=" + proposedInvalidationTimestamp +
                    '}';
        }

        long getSum(long currentTimeMillis) {
            if (!evictionPolicy.isSmoothlyEvictFromOldestChunk() || !isOldestChunk(currentTimeMillis)) {
                return sum.get();
            }
            double originalSum = sum.get();
            double notExpiredMillis = (currentTimeMillis - proposedInvalidationTimestamp) % intervalBetweenResettingMillis;
            return (long) (notExpiredMillis * originalSum / intervalBetweenResettingMillis);
        }

        boolean isOldestChunk(long currentTimeMillis) {
            return (currentTimeMillis - proposedInvalidationTimestamp) / intervalBetweenResettingMillis == chunks.length;
        }
    }

    private final class Phase {

        final AtomicLong sum;
        volatile long proposedInvalidationTimestamp;

        Phase(long proposedInvalidationTimestamp) {
            this.proposedInvalidationTimestamp = proposedInvalidationTimestamp;
            this.sum = new AtomicLong();
        }

        boolean isNeedToBeReportedToSnapshot(long currentTimeMillis) {
            if (proposedInvalidationTimestamp > currentTimeMillis) {
                return evictionPolicy.isReportUncompletedChunkToSnapshot();
            }
            long correspondentChunkProposedInvalidationTimestamp = proposedInvalidationTimestamp + (chunks.length - 1) * intervalBetweenResettingMillis;
            return correspondentChunkProposedInvalidationTimestamp > currentTimeMillis;
        }

        long getSum(long currentTimeMillis) {
            if (!evictionPolicy.isSmoothlyEvictFromOldestChunk() || !isAddressedToOldestChunk(currentTimeMillis)) {
                return sum.get();
            }
            double originalSum = sum.get();
            double notExpiredMillis = (currentTimeMillis - proposedInvalidationTimestamp) % intervalBetweenResettingMillis;
            return (long) (notExpiredMillis * originalSum / intervalBetweenResettingMillis);
        }

        boolean isAddressedToOldestChunk(long currentTimeMillis) {
            long correspondentChunkProposedInvalidationTimestamp = proposedInvalidationTimestamp + (chunks.length - 1) * intervalBetweenResettingMillis;
            return (currentTimeMillis - correspondentChunkProposedInvalidationTimestamp) / intervalBetweenResettingMillis == chunks.length - 2;
        }
    }

    @Override
    public String toString() {
        return "ResetByChunksCounter{" +
                "evictionPolicy=" + evictionPolicy +
                ", creationTimestamp=" + creationTimestamp +
                ", chunks=" + Arrays.toString(chunks) +
                ", clock=" + clock +
                ", left=" + left +
                ", right=" + right +
                ", phases=" + Arrays.toString(phases) +
                ", currentPhaseRef=" + currentPhaseRef +
                ", phaseMutators=" + phaseMutators +
                ", rotationPostponedByWriter=" + rotationPostponedByWriter +
                '}';
    }

}
