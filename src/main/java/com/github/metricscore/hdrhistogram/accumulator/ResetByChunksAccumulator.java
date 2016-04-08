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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Function;
import java.util.function.Supplier;

public class ResetByChunksAccumulator implements Accumulator {

    private final LeftRightChunk[] chunks;
    private final Clock clock;
    private final Histogram temporarySnapshotHistogram;
    private final long intervalBetweenResettingMillis;
    private final long creationTimestamp;

    public ResetByChunksAccumulator(Supplier<Recorder> recorderSupplier, int numberChunks, long intervalBetweenResettingMillis, Clock clock) {
        this.intervalBetweenResettingMillis = intervalBetweenResettingMillis;
        this.clock = clock;
        this.creationTimestamp = clock.getTime();
        this.chunks = new LeftRightChunk[numberChunks];
        for (int i = 0; i < chunks.length; i++) {
            this.chunks[i] = new LeftRightChunk(recorderSupplier, i);
        }
        this.temporarySnapshotHistogram = chunks[0].left.runningTotals.copy();
    }

    @Override
    public void recordSingleValueWithExpectedInterval(long value, long expectedIntervalBetweenValueSamples) {
        long nowMillis = clock.getTime();
        int chunkIndex = 0;
        if (chunks.length > 1) {
            long millisSinceCreation = nowMillis - creationTimestamp;
            long intervalsSinceCreation = millisSinceCreation / intervalBetweenResettingMillis;
            chunkIndex = (int) intervalsSinceCreation % chunks.length;
        }
        chunks[chunkIndex].recordValue(value, expectedIntervalBetweenValueSamples, nowMillis);
    }

    @Override
    public final synchronized Snapshot getSnapshot(Function<Histogram, Snapshot> snapshotTaker) {
        long currentTimeMillis = clock.getTime();
        temporarySnapshotHistogram.reset();
        for (LeftRightChunk chunk : chunks) {
            chunk.addActivePhaseToSnapshot(temporarySnapshotHistogram, currentTimeMillis);
        }
        return snapshotTaker.apply(temporarySnapshotHistogram);
    }

    @Override
    public int getEstimatedFootprintInBytes() {
        return temporarySnapshotHistogram.getEstimatedFootprintInBytes() * (chunks.length * 3 * 2 + 1);
    }

    private final class LeftRightChunk {

        final Phase left;
        final Phase right;
        final AtomicReference<Phase> currentPhaseRef;
        final AtomicInteger activeMutators = new AtomicInteger(0);
        volatile Runnable postponedPhaseRotation = null;

        LeftRightChunk(Supplier<Recorder> recorderSupplier, int chunkIndex) {
            left = new Phase(recorderSupplier, creationTimestamp + (chunks.length + chunkIndex) * intervalBetweenResettingMillis);
            right = new Phase(recorderSupplier, Long.MAX_VALUE);
            this.currentPhaseRef = new AtomicReference<>(left);
        }

        void addActivePhaseToSnapshot(Histogram snapshotHistogram, long currentTimeMillis) {
            while (!activeMutators.compareAndSet(0, 1)) {
                // if phase rotation process is in progress by writer thread then wait inside spin loop until rotation will done
                LockSupport.parkNanos(TimeUnit.MICROSECONDS.toNanos(500));
            }
            try {
                Phase currentPhase = currentPhaseRef.get();
                if (currentTimeMillis < currentPhase.proposedInvalidationTimestamp) {
                    currentPhase.intervalHistogram = currentPhase.recorder.getIntervalHistogram(currentPhase.intervalHistogram);
                    currentPhase.runningTotals.add(currentPhase.intervalHistogram);
                    snapshotHistogram.add(currentPhase.runningTotals);
                }
            } finally {
                if (activeMutators.decrementAndGet() > 0) {
                    while (this.postponedPhaseRotation == null) {
                        // if phase rotation process is in progress by writer thread then wait in spin loop until rotation will done
                        LockSupport.parkNanos(TimeUnit.MICROSECONDS.toNanos(500));
                    }
                    postponedPhaseRotation.run();
                }
            }
        }

        void recordValue(long value, long expectedIntervalBetweenValueSamples, long currentTimeMillis) {
            Phase currentPhase = currentPhaseRef.get();
            if (currentTimeMillis < currentPhase.proposedInvalidationTimestamp) {
                currentPhase.recorder.recordValueWithExpectedInterval(value, expectedIntervalBetweenValueSamples);
                return;
            }

            Phase nextPhase = currentPhase == left ? right : left;
            if (!currentPhaseRef.compareAndSet(currentPhase, nextPhase)) {
                // another writer achieved progress and must clear current phase data, current writer tread just can write value to next phase and return
                nextPhase.recorder.recordValueWithExpectedInterval(value, expectedIntervalBetweenValueSamples);
                return;
            }

            // Current thread is responsible to rotate phases.
            Runnable phaseRotation = () -> {
                postponedPhaseRotation = null;
                long millisSinceCreation = currentTimeMillis - creationTimestamp;
                long intervalsSinceCreation = millisSinceCreation / intervalBetweenResettingMillis;
                currentPhase.recorder.reset();
                currentPhase.runningTotals.reset();
                currentPhase.proposedInvalidationTimestamp = Long.MAX_VALUE;
                nextPhase.recorder.recordValueWithExpectedInterval(value, expectedIntervalBetweenValueSamples);
                activeMutators.decrementAndGet();
                nextPhase.proposedInvalidationTimestamp = creationTimestamp + (intervalsSinceCreation + chunks.length) * intervalBetweenResettingMillis;
            };

            // Need to be aware about snapshot takers in the middle of progress state
            if (activeMutators.incrementAndGet() > 1) {
                // give chance to snapshot taker to finalize snapshot extraction, rotation will be complete by snapshot taker thread
                postponedPhaseRotation = phaseRotation;
                return;
            }

            // There are no active snapshot takers in the progress state, lets exchange phases in this writer thread
            phaseRotation.run();
        }
    }

    private static final class Phase {

        final Histogram runningTotals;
        final Recorder recorder;

        Histogram intervalHistogram;
        volatile long proposedInvalidationTimestamp;

        Phase(Supplier<Recorder> recorderSupplier, long proposedInvalidationTimestamp) {
            this.recorder = recorderSupplier.get();
            this.intervalHistogram = recorder.getIntervalHistogram();
            this.runningTotals = intervalHistogram.copy();
            this.proposedInvalidationTimestamp = proposedInvalidationTimestamp;
        }
    }

}
