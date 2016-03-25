package com.github.metricscore.hdrhistogram;

import com.codahale.metrics.Snapshot;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

class ResetPeriodicallyAccumulationStrategy implements AccumulationStrategy {

    private final long resetIntervalMillis;

    ResetPeriodicallyAccumulationStrategy(Duration resetPeriod) {
        if (resetPeriod.isNegative() || resetPeriod.isZero()) {
            throw new IllegalArgumentException("resetPeriod must be a positive duration");
        }
        this.resetIntervalMillis = resetPeriod.toMillis();
    }

    @Override
    public Accumulator createAccumulator(Recorder recorder, WallClock wallClock) {
        return new ResetPeriodicallyAccumulator(recorder, resetIntervalMillis, wallClock);
    }

    private static class ResetPeriodicallyAccumulator implements Accumulator {

        private static final long RESETTING_IN_PROGRESS_HAZARD = Long.MIN_VALUE;

        final Lock lock = new ReentrantLock();
        final Recorder recorder;
        final Histogram uniformHistogram;
        final long resetIntervalMillis;
        final WallClock wallClock;
        final AtomicLong nextResetTimeMillisRef;

        Histogram intervalHistogram;

        public ResetPeriodicallyAccumulator(Recorder recorder, long resetIntervalMillis, WallClock wallClock) {
            this.resetIntervalMillis = resetIntervalMillis;
            this.wallClock = wallClock;
            this.recorder = recorder;
            lock.lock();
            try {
                this.intervalHistogram = recorder.getIntervalHistogram();
            } finally {
                lock.unlock();
            }
            this.uniformHistogram = intervalHistogram.copy();
            nextResetTimeMillisRef = new AtomicLong(wallClock.currentTimeMillis() + resetIntervalMillis);
        }

        @Override
        public void recordValue(long value) {
            resetIfNeed();
            recorder.recordValue(value);
        }

        @Override
        public Snapshot getSnapshot(Function<Histogram, Snapshot> snapshotTaker) {
            lock.lock();
            try {
                resetIfNeed();
                intervalHistogram = recorder.getIntervalHistogram(intervalHistogram);
                uniformHistogram.add(intervalHistogram);
                return snapshotTaker.apply(uniformHistogram);
            } finally {
                lock.unlock();
            }
        }

        private void resetIfNeed() {
            long nextResetTimeMillis = nextResetTimeMillisRef.get();
            if (nextResetTimeMillis != RESETTING_IN_PROGRESS_HAZARD && wallClock.currentTimeMillis() >= nextResetTimeMillis) {
                if (!nextResetTimeMillisRef.compareAndSet(nextResetTimeMillis, RESETTING_IN_PROGRESS_HAZARD)) {
                    // another concurrent thread achieved progress and became responsible for resetting histograms
                    return;
                }
                // CAS was successful, so current thread became the responsible for resetting histograms
                lock.lock();
                try {
                    intervalHistogram = recorder.getIntervalHistogram(intervalHistogram);
                    uniformHistogram.reset();
                    nextResetTimeMillisRef.set(wallClock.currentTimeMillis() + resetIntervalMillis);
                } finally {
                    lock.unlock();
                }
            }
        }
    }

}
