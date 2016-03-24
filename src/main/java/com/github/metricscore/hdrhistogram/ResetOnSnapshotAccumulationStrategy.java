package com.github.metricscore.hdrhistogram;

import com.codahale.metrics.Snapshot;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

class ResetOnSnapshotAccumulationStrategy implements AccumulationStrategy {

    public static AccumulationStrategy INSTANCE = new ResetOnSnapshotAccumulationStrategy();

    private ResetOnSnapshotAccumulationStrategy() {}

    @Override
    public Accumulator createAccumulator(Recorder recorder) {
        return new ResetOnSnapshotAccumulator(recorder);
    }

    private static class ResetOnSnapshotAccumulator implements Accumulator {
        private final Lock lock;
        private final Recorder recorder;

        private Histogram intervalHistogram;

        public ResetOnSnapshotAccumulator(Recorder recorder) {
            this.lock = new ReentrantLock();
            this.recorder = recorder;

            lock.lock();
            try {
                this.intervalHistogram = recorder.getIntervalHistogram();
            } finally {
                lock.unlock();
            }
        }

        @Override
        public void recordValue(long value) {
            recorder.recordValue(value);
        }

        @Override
        public Snapshot getSnapshot(Function<Histogram, Snapshot> snapshotTaker) {
            lock.lock();
            try {
                intervalHistogram = recorder.getIntervalHistogram(intervalHistogram);
                return snapshotTaker.apply(intervalHistogram);
            } finally {
                lock.unlock();
            }
        }
    }

}
