/*
 *    Copyright 2016 Vladimir Bukhtoyarov
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

package com.github.metricscore.hdr.counter.resetByChunks;

import com.github.metricscore.hdr.ChunkEvictionPolicy;
import com.github.metricscore.hdr.counter.WindowCounter;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class ResetAtSnapshotCounterCombinedBenchmark {

    @State(Scope.Benchmark)
    public static class CounterState {
        public final WindowCounter counter = WindowCounter.newResetByChunkCounter(new ChunkEvictionPolicy(Duration.ofSeconds(1), 10));
    }

    @Benchmark
    @Group("readSumWithContendedWrite")
    @GroupThreads(3)
    public void add(CounterState state) {
        state.counter.add(42);
    }

    @Benchmark
    @Group("readSumWithContendedWrite")
    @GroupThreads(1)
    public long readSum(CounterState state) {
        return state.counter.getSum();
    }

    public static void main(String[] args) throws RunnerException {
        runBenchmark(4, ResetAtSnapshotCounterCombinedBenchmark.class);
    }

    private static void runBenchmark(int threadCount, Class benchmarkClass) {
        Options opt = new OptionsBuilder()
                .include(benchmarkClass.getSimpleName())
                .warmupIterations(5)
                .measurementIterations(5)
                .threads(threadCount)
                .forks(1)
                .build();
        try {
            new Runner(opt).run();
        } catch (RunnerException e) {
            throw new RuntimeException(e);
        }
    }

}
