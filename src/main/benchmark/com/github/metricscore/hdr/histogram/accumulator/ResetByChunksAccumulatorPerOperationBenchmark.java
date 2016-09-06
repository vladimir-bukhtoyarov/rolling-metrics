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

package com.github.metricscore.hdr.histogram.accumulator;

import com.codahale.metrics.ExponentiallyDecayingReservoir;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Snapshot;
import com.github.metricscore.hdr.RunnerUtil;
import com.github.metricscore.hdr.histogram.HdrBuilder;
import org.HdrHistogram.ConcurrentHistogram;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.RunnerException;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class ResetByChunksAccumulatorPerOperationBenchmark {

    @State(Scope.Benchmark)
    public static class AddState {
        public final Histogram chunkedHistogram = new HdrBuilder()
                .resetReservoirByChunks(Duration.ofSeconds(10), 7)
                .buildHistogram();

        public final Histogram metricsCoreHistogram = new Histogram(new ExponentiallyDecayingReservoir());

        public final ConcurrentHistogram hdrHistogram = new ConcurrentHistogram(2);

    }

    @State(Scope.Benchmark)
    public static class GetSnapshotState {
        public final Histogram chunkedHistogram = new HdrBuilder()
                .resetReservoirByChunks(Duration.ofSeconds(10), 7)
                .buildHistogram();

        public final Histogram metricsCoreHistogram = new Histogram(new ExponentiallyDecayingReservoir());

        {
            for (int i = 0; i < 7; i++) {
                for (int j = 0; j < 2000; j++) {
                    long random = ThreadLocalRandom.current().nextLong(3000);
                    chunkedHistogram.update(random);
                    metricsCoreHistogram.update(random);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

        }

    }

    @Benchmark
    public long baseLine(AddState state) {
        return ThreadLocalRandom.current().nextLong(10000);
    }

    @Benchmark
    public void metricsCoreAdd(AddState state) {
        state.metricsCoreHistogram.update(ThreadLocalRandom.current().nextLong(10000));
    }

    @Benchmark
    public void chunkedHistogramAdd(AddState state) {
        state.chunkedHistogram.update(ThreadLocalRandom.current().nextLong(10000));
    }

    @Benchmark
    public void hdrHistogramAdd(AddState state) {
        state.hdrHistogram.recordValue(ThreadLocalRandom.current().nextLong(10000));
    }

    @Benchmark
    public Snapshot getMetricsCoreSnapshot(GetSnapshotState state) {
        return state.metricsCoreHistogram.getSnapshot();
    }

    @Benchmark
    public Snapshot getChunkedHistogramSnapshot(GetSnapshotState state) {
        return state.chunkedHistogram.getSnapshot();
    }

    public static class OneThread {
        public static void main(String[] args) throws RunnerException {
            RunnerUtil.runBenchmark(1, ResetByChunksAccumulatorPerOperationBenchmark.class);
        }
    }

    public static class FourThread {
        public static void main(String[] args) throws RunnerException {
            RunnerUtil.runBenchmark(4, ResetByChunksAccumulatorPerOperationBenchmark.class);
        }
    }

}
