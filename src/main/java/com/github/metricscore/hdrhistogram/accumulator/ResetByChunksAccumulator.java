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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

public class ResetByChunksAccumulator implements Accumulator {

    private final Chunk[] chunks;
    private final Clock clock;
    private final Histogram temporarySnapshotHistogram;
    private final long intervalBetweenResettingMillis;
    private final long creationTimestamp;

    public ResetByChunksAccumulator(Supplier<Recorder> recorderSupplier, int numberChunks, long intervalBetweenResettingMillis, Clock clock) {
        this.intervalBetweenResettingMillis = intervalBetweenResettingMillis;
        this.clock = clock;
        this.creationTimestamp = clock.getTime();
        this.chunks = new Chunk[numberChunks];
        for (int i = 0; i < chunks.length; i++) {
            this.chunks[i] = new Chunk(recorderSupplier, i);
        }
        this.temporarySnapshotHistogram = chunks[0].phases[0].runningTotals.copy();
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
        chunks[chunkIndex].recordValueAndReturnActivePhase(value, expectedIntervalBetweenValueSamples, nowMillis);
    }

    @Override
    public Snapshot getSnapshot(Function<Histogram, Snapshot> snapshotTaker) {
        synchronized (this) {
            temporarySnapshotHistogram.reset();
            long nowMillis = clock.getTime();
            for (Chunk chunk : chunks) {
                chunk.addActivePhaseToSnapshot(temporarySnapshotHistogram, nowMillis);
            }
            return snapshotTaker.apply(temporarySnapshotHistogram);
        }
    }

    @Override
    public int getEstimatedFootprintInBytes() {
        return temporarySnapshotHistogram.getEstimatedFootprintInBytes() * (chunks.length * 3 + 1);
    }

    private final class Chunk {

        final Phase[] phases;
        final AtomicInteger currentPhaseIndexRef;

        public Chunk(Supplier<Recorder> recorderSupplier, int chunkIndex) {
            this.phases = new Phase[2];
            phases[1] = new Phase(recorderSupplier, creationTimestamp + (chunkIndex + 1) * intervalBetweenResettingMillis);
            phases[1] = new Phase(recorderSupplier, Long.MAX_VALUE);
            this.currentPhaseIndexRef = new AtomicInteger(0);
        }

        public void addActivePhaseToSnapshot(Histogram snapshotHistogram, long nowMillis) {
            Phase activePhase = recordValueAndReturnActivePhase(-1, -1, nowMillis);
            /*
            Synchronization is added to this place just to handle unlikely trouble when JVM stopped in the middle of snapshot creation for the pause longest than chunkCount * intervalBetweenResettingMillis.
            It is unlikely to have real contention in this place,
            but when all goes bad(like long GC pauses), the contention on in the currentPhase it is not the worst thing that happens to your application.
              */
            synchronized (activePhase) {
                activePhase.intervalHistogram = activePhase.recorder.getIntervalHistogram(activePhase.intervalHistogram);
                activePhase.runningTotals.add(activePhase.intervalHistogram);
                snapshotHistogram.add(activePhase.runningTotals);
            }
        }

        public Phase recordValueAndReturnActivePhase(long value, long expectedIntervalBetweenValueSamples, long nowMillis) {
            int currentPhaseIndex = currentPhaseIndexRef.get();
            Phase currentPhase = phases[currentPhaseIndex];
            if (nowMillis >= currentPhase.proposedInvalidationDateMillis) {
                int nextPhaseIndex = currentPhaseIndex == 0 ? 1 : 0;
                if (currentPhaseIndexRef.compareAndSet(currentPhaseIndex, nextPhaseIndex)) {
                    // CAS was successful, so current thread became responsible for rotation

                    /*
                    Synchronization is added to this place just to handle unlikely trouble when JVM stopped in the middle of snapshot creation for the pause longest than chunkCount * intervalBetweenResettingMillis.
                    It is unlikely to have real contention in this place,
                    but when all goes bad(like long GC pauses), the contention on in the currentPhase it is not the worst thing that happens to your application.
                      */
                    synchronized (currentPhase) {
                        currentPhase.recorder.reset();
                        currentPhase.runningTotals.reset();
                        currentPhase.proposedInvalidationDateMillis = Long.MAX_VALUE;
                    }

                    Phase nextPhase = phases[nextPhaseIndex];
                    if (value >= 0) {
                        nextPhase.recorder.recordValueWithExpectedInterval(value, expectedIntervalBetweenValueSamples);
                    }

                    long millisSinceCreation = nowMillis - creationTimestamp;
                    long intervalsSinceCreation = millisSinceCreation / intervalBetweenResettingMillis;
                    nextPhase.proposedInvalidationDateMillis = creationTimestamp + (intervalsSinceCreation + 1) * intervalBetweenResettingMillis;

                    return nextPhase;
                } else {
                    // another parallel thread has achieved progress and became responsible for rotation
                    currentPhase = phases[nextPhaseIndex];
                }
            }
            if (value >= 0) {
                currentPhase.recorder.recordValueWithExpectedInterval(value, expectedIntervalBetweenValueSamples);
            }
            return currentPhase;
        }
    }

    private static final class Phase {
        final Histogram runningTotals;
        final Recorder recorder;

        Histogram intervalHistogram;
        volatile long proposedInvalidationDateMillis;

        public Phase(Supplier<Recorder> recorderSupplier, long proposedInvalidationDateMillis) {
            this.recorder = recorderSupplier.get();
            this.intervalHistogram = recorder.getIntervalHistogram();
            this.runningTotals = intervalHistogram.copy();
            this.proposedInvalidationDateMillis = proposedInvalidationDateMillis;
        }
    }

}
