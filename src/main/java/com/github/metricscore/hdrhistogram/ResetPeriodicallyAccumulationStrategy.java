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

    private final WallClock wallClock;
    private final long resetIntervalMillis;

    ResetPeriodicallyAccumulationStrategy(Duration resetPeriod, WallClock wallClock) {
        if (resetPeriod.isNegative() || resetPeriod.isZero()) {
            throw new IllegalArgumentException("Wrong reset period " + resetPeriod);
        }
        this.wallClock = wallClock;
        this.resetIntervalMillis = resetPeriod.toMillis();
    }

    @Override
    public Accumulator createAccumulator(Recorder recorder) {
        return new ResetPeriodicallyAccumulator(recorder, resetIntervalMillis, wallClock);
    }

    private static class ResetPeriodicallyAccumulator implements Accumulator {
        private final Lock lock = new ReentrantLock();
        private final Recorder recorder;
        private final Histogram uniformHistogram;
        private final long resetIntervalMillis;
        private final WallClock wallClock;
        private final AtomicLong nextResetTimeMillisRef;

        Histogram intervalHistogram;

        public ResetPeriodicallyAccumulator(Recorder recorder, long resetIntervalMillis, WallClock wallClock) {
            this.resetIntervalMillis = resetIntervalMillis;
            this.wallClock = wallClock;
            this.recorder = recorder;
            this.intervalHistogram = recorder.getIntervalHistogram();
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
            resetIfNeed();

            lock.lock();
            try {
                intervalHistogram = recorder.getIntervalHistogram(intervalHistogram);
                uniformHistogram.add(intervalHistogram);
                return snapshotTaker.apply(uniformHistogram);
            } finally {
                lock.unlock();
            }
        }

        private void resetIfNeed() {
            long nowMillis = wallClock.currentTimeMillis();
            long nextResetTimeMillis = nextResetTimeMillisRef.get();
            if (nowMillis >= nextResetTimeMillis) {
                lock.lock();
                try {
                    nextResetTimeMillis = nextResetTimeMillisRef.get()
                    if (nowMillis >= nextResetTimeMillis) {
                        intervalHistogram = recorder.getIntervalHistogram(intervalHistogram);
                        uniformHistogram.reset();
                        long proposedNextResetTimeMillis
                        nextResetTimeMillisRef.set(nextResetTimeMillis, nowMillis +)
                    }
                } finally {
                    lock.unlock();
                }
                nextResetTimeMillis += resetIntervalMillis;
            }
        }
    }

}
