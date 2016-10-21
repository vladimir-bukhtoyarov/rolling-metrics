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
import com.github.metricscore.hdr.histogram.HdrBuilder;
import com.github.metricscore.hdr.histogram.OverflowResolver;
import org.HdrHistogram.ConcurrentHistogram;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class ResetByChunksAccumulatorPerOperationBenchmark {

    @State(Scope.Benchmark)
    public static class ChunkedHistogramState {
        public final Histogram chunkedHistogram = new HdrBuilder()
                .resetReservoirByChunksWithRollingTimeWindow(Duration.ofSeconds(10), 7)
                .buildHistogram();
    }

    @State(Scope.Benchmark)
    public static class ChunkedUpperLimitedHistogramState {
        public final Histogram chunkedHistogram = new HdrBuilder()
                .resetReservoirByChunksWithRollingTimeWindow(Duration.ofSeconds(10), 7)
                .withHighestTrackableValue(5000, OverflowResolver.REDUCE_TO_HIGHEST_TRACKABLE)
                .buildHistogram();
    }

    @State(Scope.Benchmark)
    public static class MetricsHistogramState {

        public final Histogram metricsCoreHistogram = new Histogram(new ExponentiallyDecayingReservoir());

    }

    @State(Scope.Benchmark)
    public static class HdrHistogramState {

        public final ConcurrentHistogram hdrHistogram = new ConcurrentHistogram(2);

    }

    @State(Scope.Benchmark)
    public static class GetChunkedSnapshotState {
        public final Histogram chunkedHistogram = new HdrBuilder()
                .resetReservoirByChunksWithRollingTimeWindow(Duration.ofSeconds(10), 7)
                .buildHistogram();

        @Setup
        public void setup() {
            for (int i = 0; i < 7; i++) {
                for (int j = 0; j < 2000; j++) {
                    long random = ThreadLocalRandom.current().nextLong(3000);
                    chunkedHistogram.update(random);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

        }

    }

    @State(Scope.Benchmark)
    public static class GetChunkedUpperLimitedSnapshotState {
        public final Histogram chunkedHistogram = new HdrBuilder()
                .resetReservoirByChunksWithRollingTimeWindow(Duration.ofSeconds(10), 7)
                .withHighestTrackableValue(5000, OverflowResolver.REDUCE_TO_HIGHEST_TRACKABLE)
                .buildHistogram();

        @Setup
        public void setup() {
            for (int i = 0; i < 7; i++) {
                for (int j = 0; j < 2000; j++) {
                    long random = ThreadLocalRandom.current().nextLong(3000);
                    chunkedHistogram.update(random);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

        }

    }

    @State(Scope.Benchmark)
    public static class GetMetricsSnapshotState {

        public final Histogram metricsCoreHistogram = new Histogram(new ExponentiallyDecayingReservoir());

        @Setup
        public void setup() {
            for (int i = 0; i < 7; i++) {
                for (int j = 0; j < 2000; j++) {
                    long random = ThreadLocalRandom.current().nextLong(3000);
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
    public long baseLine() {
        return ThreadLocalRandom.current().nextLong(10000);
    }

    @Benchmark
    public void metricsCoreAdd(MetricsHistogramState state) {
        state.metricsCoreHistogram.update(ThreadLocalRandom.current().nextLong(10000));
    }

    @Benchmark
    public void chunkedHistogramAdd(ChunkedHistogramState state) {
        state.chunkedHistogram.update(ThreadLocalRandom.current().nextLong(10000));
    }

    @Benchmark
    public void chunkedUpperLimitedHistogramAdd(ChunkedUpperLimitedHistogramState state) {
        state.chunkedHistogram.update(ThreadLocalRandom.current().nextLong(10000));
    }

    @Benchmark
    public void hdrHistogramAdd(HdrHistogramState state) {
        state.hdrHistogram.recordValue(ThreadLocalRandom.current().nextLong(10000));
    }

    @Benchmark
    public Snapshot getMetricsCoreSnapshot(GetMetricsSnapshotState state) {
        return state.metricsCoreHistogram.getSnapshot();
    }

    @Benchmark
    public Snapshot getChunkedUpperLimitedHistogramSnapshot(GetChunkedUpperLimitedSnapshotState state) {
        return state.chunkedHistogram.getSnapshot();
    }

    @Benchmark
    public Snapshot getChunkedHistogramSnapshot(GetChunkedSnapshotState state) {
        return state.chunkedHistogram.getSnapshot();
    }

    public static class OneThread {
        public static void main(String[] args) throws RunnerException {
            Options opt = new OptionsBuilder()
                    .include(((Class) ResetByChunksAccumulatorPerOperationBenchmark.class).getSimpleName())
                    .warmupIterations(5)
                    .measurementIterations(5)
                    .threads(1)
                    .forks(1)
                    .build();
            try {
                new Runner(opt).run();
            } catch (RunnerException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class FourThread {
        public static void main(String[] args) throws RunnerException {
            Options opt = new OptionsBuilder()
                    .include(((Class) ResetByChunksAccumulatorPerOperationBenchmark.class).getSimpleName())
                    .warmupIterations(5)
                    .measurementIterations(5)
                    .threads(4)
                    .forks(1)
                    .build();
            try {
                new Runner(opt).run();
            } catch (RunnerException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
