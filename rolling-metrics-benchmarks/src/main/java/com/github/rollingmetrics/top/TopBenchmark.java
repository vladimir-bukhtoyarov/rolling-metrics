/*
 *
 *  Copyright 2017 Vladimir Bukhtoyarov
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

package com.github.rollingmetrics.top;


import com.github.rollingmetrics.retention.RetentionPolicy;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class TopBenchmark {

    @State(Scope.Benchmark)
    public static class TopState {

        final Top chunkedTop_1 = RetentionPolicy
                .resetPeriodicallyByChunks(Duration.ofSeconds(4), 4)
                .newTopBuilder(1)
                .build();

        final Top chunkedTop_10 = RetentionPolicy
                .resetPeriodicallyByChunks(Duration.ofSeconds(4), 4)
                .newTopBuilder(10)
                .build();

        final Top periodicallyTop_1 = RetentionPolicy
                .resetPeriodically(Duration.ofSeconds(1))
                .newTopBuilder(1)
                .build();

        final Top periodicallyTop_10 = RetentionPolicy
                .resetPeriodically(Duration.ofSeconds(1))
                .newTopBuilder(10)
                .build();

        final Top resetOnSnapshotTop_1 = RetentionPolicy.resetOnSnapshot()
                .newTopBuilder(1)
                .build();

        final Top resetOnSnapshotTop_10 = RetentionPolicy.resetOnSnapshot()
                .newTopBuilder(10)
                .build();

        final Top uniformTop_1 = RetentionPolicy.uniform()
                .newTopBuilder(1)
                .build();

        final Top uniformTop_10 = RetentionPolicy.uniform()
                .newTopBuilder(10)
                .build();
    }

    @Benchmark
    public long baseLine() {
        return getRandomValue();
    }

    @Group("chunkedTop_1")
    @GroupThreads(3)
    @Benchmark
    public void update_chunkedTop_1(TopState state) {
        state.chunkedTop_1.update(0, getRandomValue(), TimeUnit.NANOSECONDS, () -> "Some query to something");
    }

    @Group("chunkedTop_1")
    @GroupThreads(1)
    @Benchmark
    public List<Position> getSnapshot_chunkedTop_1(TopState state) {
        return state.chunkedTop_1.getPositionsInDescendingOrder();
    }

    @Group("chunkedTop_10")
    @GroupThreads(3)
    @Benchmark
    public void update_chunkedTop_10(TopState state) {
        state.chunkedTop_10.update(0, getRandomValue(), TimeUnit.NANOSECONDS, () -> "Some query to something");
    }

    @Group("chunkedTop_10")
    @GroupThreads(1)
    @Benchmark
    public List<Position> getSnapshot_chunkedTop_10(TopState state) {
        return state.chunkedTop_10.getPositionsInDescendingOrder();
    }

    @Group("periodicallyTop_1")
    @GroupThreads(3)
    @Benchmark
    public void update_periodicallyTop_1(TopState state) {
        state.periodicallyTop_1.update(0, getRandomValue(), TimeUnit.NANOSECONDS, () -> "Some query to something");
    }

    @Group("periodicallyTop_1")
    @GroupThreads(1)
    @Benchmark
    public List<Position> getSnapshot_periodicallyTop_1(TopState state) {
        return state.periodicallyTop_1.getPositionsInDescendingOrder();
    }

    @Group("periodicallyTop_10")
    @GroupThreads(3)
    @Benchmark
    public void update_periodicallyTop_10(TopState state) {
        state.periodicallyTop_10.update(0, getRandomValue(), TimeUnit.NANOSECONDS, () -> "Some query to something");
    }

    @Group("periodicallyTop_10")
    @GroupThreads(1)
    @Benchmark
    public List<Position> getSnapshot_periodicallyTop_10(TopState state) {
        return state.periodicallyTop_10.getPositionsInDescendingOrder();
    }

    @Group("resetOnSnapshotTop_1")
    @GroupThreads(3)
    @Benchmark
    public void update_resetOnSnapshotTop_1(TopState state) {
        state.resetOnSnapshotTop_1.update(0, getRandomValue(), TimeUnit.NANOSECONDS, () -> "Some query to something");
    }

    @Group("resetOnSnapshotTop_1")
    @GroupThreads(1)
    @Benchmark
    public List<Position> getSnapshot_resetOnSnapshotTop_1(TopState state) {
        return state.resetOnSnapshotTop_1.getPositionsInDescendingOrder();
    }

    @Group("resetOnSnapshotTop_10")
    @GroupThreads(3)
    @Benchmark
    public void update_resetOnSnapshotTop_10(TopState state) {
        state.resetOnSnapshotTop_10.update(0, getRandomValue(), TimeUnit.NANOSECONDS, () -> "Some query to something");
    }

    @Group("resetOnSnapshotTop_10")
    @GroupThreads(1)
    @Benchmark
    public List<Position> getSnapshot_resetOnSnapshotTop_10(TopState state) {
        return state.resetOnSnapshotTop_10.getPositionsInDescendingOrder();
    }

    @Group("uniformTop_1")
    @GroupThreads(3)
    @Benchmark
    public void update_uniformTop_1(TopState state) {
        state.uniformTop_1.update(0, getRandomValue(), TimeUnit.NANOSECONDS, () -> "Some query to something");
    }

    @Group("uniformTop_1")
    @GroupThreads(1)
    @Benchmark
    public List<Position> getSnapshot_uniformTop_1(TopState state) {
        return state.uniformTop_1.getPositionsInDescendingOrder();
    }

    @Group("uniformTop_10")
    @GroupThreads(3)
    @Benchmark
    public void update_uniformTop_10(TopState state) {
        state.uniformTop_10.update(0, getRandomValue(), TimeUnit.NANOSECONDS, () -> "Some query to something");
    }

    @Group("uniformTop_10")
    @GroupThreads(1)
    @Benchmark
    public List<Position> getSnapshot_uniformTop_10(TopState state) {
        return state.uniformTop_10.getPositionsInDescendingOrder();
    }

    private static long getRandomValue() {
        return ThreadLocalRandom.current().nextLong(15_000_000) + 5_000_000;
    }

    public static class FourThread {
        public static void main(String[] args) throws RunnerException {
            Options opt = new OptionsBuilder()
                    .include((TopBenchmark.class).getSimpleName())
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
