/*
 *    Copyright 2017 Vladimir Bukhtoyarov
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package com.github.rollingmetrics.histogram.hdr.impl;

import com.github.rollingmetrics.histogram.hdr.RollingSnapshot;
import org.HdrHistogram.Histogram;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;


@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class CountAtPercentileExtractionBenchmark {

    @State(Scope.Benchmark)
    public static class HistogramState {

        Histogram histogram = new Histogram(1000, 3600L * 1_000_000_000L, 2);
        static double[] DEFAULT_PERCENTILES_7 = new double[]{0.5, 0.75, 0.9, 0.95, 0.98, 0.99, 0.999};
        static double[] DEFAULT_PERCENTILES_1 = new double[]{0.999};

        @Setup
        public void setup() {
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 1024; j++) {
                    // generate latency between 5ms and 20ms
                    long randomNanos = ThreadLocalRandom.current().nextLong(15_000_000) + 5_000_000;
                    histogram.recordValue(randomNanos);
                }
            }
        }
    }

    @Benchmark
    public RollingSnapshot calculatePercentile_7(HistogramState state) {
        return AbstractRollingHdrHistogram.takeSmartSnapshot(HistogramState.DEFAULT_PERCENTILES_7, state.histogram);
    }

    @Benchmark
    public RollingSnapshot calculatePercentile_1(HistogramState state) {
        return AbstractRollingHdrHistogram.takeSmartSnapshot(HistogramState.DEFAULT_PERCENTILES_1, state.histogram);
    }

    public static class OneThread {
        public static void main(String[] args) throws RunnerException {
            Options opt = new OptionsBuilder()
                    .include((CountAtPercentileExtractionBenchmark.class).getSimpleName())
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
