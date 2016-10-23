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
import com.github.metricscore.hdr.util.Clock;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class HistogramSnapshotExtractionBenchmark {

    @org.openjdk.jmh.annotations.State(Scope.Benchmark)
    public static class StateWithMockClock {

        final AtomicLong currentTimeMillis = new AtomicLong(System.currentTimeMillis());
        final Clock clock = Clock.mock(currentTimeMillis);

        final Histogram chunkedHistogram = new HdrBuilder(clock)
                .resetReservoirPeriodicallyByChunks(Duration.ofSeconds(3), 3)
                .buildHistogram();

        final Histogram upperLimitedChunkedHistogram = new HdrBuilder(clock)
                .resetReservoirPeriodicallyByChunks(Duration.ofSeconds(3), 3)
                .withLowestDiscernibleValue(TimeUnit.MICROSECONDS.toNanos(1))
                .withHighestTrackableValue(TimeUnit.MINUTES.toNanos(5), OverflowResolver.REDUCE_TO_HIGHEST_TRACKABLE)
                .buildHistogram();

        @Setup
        public void setup() {
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 1024; j++) {
                    // generate latency between 5ms and 20ms
                    long randomNanos = ThreadLocalRandom.current().nextLong(15_000_000) + 5_000_000;

                    chunkedHistogram.update(randomNanos);
                    upperLimitedChunkedHistogram.update(randomNanos);
                }
                currentTimeMillis.addAndGet(1000);
            }
        }
    }

    @org.openjdk.jmh.annotations.State(Scope.Benchmark)
    public static class StateWithRealClock {

        final Histogram resetOnSnapshotHistogram = new HdrBuilder()
                .resetReservoirOnSnapshot()
                .withLowestDiscernibleValue(TimeUnit.MICROSECONDS.toNanos(1))
                .withHighestTrackableValue(TimeUnit.MINUTES.toNanos(5), OverflowResolver.REDUCE_TO_HIGHEST_TRACKABLE)
                .buildHistogram();

        final Histogram resetPeriodicallyHistogram = new HdrBuilder()
                .resetReservoirPeriodically(Duration.ofSeconds(300))
                .withLowestDiscernibleValue(TimeUnit.MICROSECONDS.toNanos(1))
                .withHighestTrackableValue(TimeUnit.MINUTES.toNanos(5), OverflowResolver.REDUCE_TO_HIGHEST_TRACKABLE)
                .buildHistogram();

        final Histogram uniformHistogram = new HdrBuilder()
                .neverResetReservoir()
                .withLowestDiscernibleValue(TimeUnit.MICROSECONDS.toNanos(1))
                .withHighestTrackableValue(TimeUnit.MINUTES.toNanos(5), OverflowResolver.REDUCE_TO_HIGHEST_TRACKABLE)
                .buildHistogram();

        final Histogram metricsCoreHistogram = new Histogram(new ExponentiallyDecayingReservoir());

        @Setup
        public void setup() {
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 1024; j++) {
                    // generate latency between 5ms and 20ms
                    long randomNanos = ThreadLocalRandom.current().nextLong(15_000_000) + 5_000_000;

                    resetOnSnapshotHistogram.update(randomNanos);
                    resetPeriodicallyHistogram.update(randomNanos);
                    uniformHistogram.update(randomNanos);
                    metricsCoreHistogram.update(randomNanos);
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
    public Map<String, Object> getMetricsCoreHistogramSnapshot(StateWithRealClock state) {
        return getSnaphsotRepresentation(state.metricsCoreHistogram);
    }

    @Benchmark
    public Map<String, Object> getUniformHistogramSnapshot(StateWithRealClock state) {
        return getSnaphsotRepresentation(state.uniformHistogram);
    }

    @Benchmark
    public Map<String, Object> getResetPeriodicallyHistogramSnapshot(StateWithRealClock state) {
        return getSnaphsotRepresentation(state.resetPeriodicallyHistogram);
    }

    @Benchmark
    public Map<String, Object> getResetOnSnapshotHistogramSnapshot(StateWithRealClock state) {
        return getSnaphsotRepresentation(state.resetOnSnapshotHistogram);
    }

    @Benchmark
    public Map<String, Object> getChunkedHistogramSnapshot(StateWithMockClock state) {
        return getSnaphsotRepresentation(state.chunkedHistogram);
    }

    @Benchmark
    public Map<String, Object> getChunkedUpperLimitedHistogramSnapshot(StateWithMockClock state) {
        return getSnaphsotRepresentation(state.upperLimitedChunkedHistogram);
    }

    private static Map<String, Object> getSnaphsotRepresentation(Histogram histogram) {
        Map<String, Object> view = new HashMap<>();
        Snapshot snapshot = histogram.getSnapshot();
        view.put("max", snapshot.getMax());
        view.put("min", snapshot.getMin());
        view.put("mean", snapshot.getMean());
        view.put("median", snapshot.getMedian());
        view.put("stdDeviation", snapshot.getStdDev());
        view.put("75", snapshot.get75thPercentile());
        view.put("95", snapshot.get95thPercentile());
        view.put("98", snapshot.get98thPercentile());
        view.put("99", snapshot.get99thPercentile());
        view.put("999", snapshot.get999thPercentile());
        return view;
    }

    public static class OneThread {
        public static void main(String[] args) throws RunnerException {
            Options opt = new OptionsBuilder()
                    .include(((Class) HistogramSnapshotExtractionBenchmark.class).getSimpleName())
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

}
