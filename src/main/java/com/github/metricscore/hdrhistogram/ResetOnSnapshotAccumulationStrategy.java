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
    public Accumulator createAccumulator(Recorder recorder, WallClock wallClock) {
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
