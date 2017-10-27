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

package com.github.rollingmetrics.counter;

import com.github.rollingmetrics.util.BackgroundTicker;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class SmoothlyDecayingRollingCounterBenchmark {

    @State(Scope.Benchmark)
    public static class CounterState {

        BackgroundTicker backgroundClock;
        public WindowCounter counter;
        public WindowCounter counterWithBackgroundClock;

        @Setup
        public void setup() {
            backgroundClock = new BackgroundTicker(100);
            counterWithBackgroundClock = new SmoothlyDecayingRollingCounter(Duration.ofMillis(1000), 10, backgroundClock);
            counter = new SmoothlyDecayingRollingCounter(Duration.ofMillis(1000), 10);
        }

        @TearDown
        public void tearDown() {
            backgroundClock.stop();
        }
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

    @Benchmark
    @Group("readSumWithContendedWrite_backgroundClock")
    @GroupThreads(3)
    public void add_withBackgroundClock(CounterState state) {
        state.counter.add(42);
    }

    @Benchmark
    @Group("readSumWithContendedWrite_backgroundClock")
    @GroupThreads(1)
    public long readSum_withBackgroundClock(CounterState state) {
        return state.counter.getSum();
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(((Class) SmoothlyDecayingRollingCounterBenchmark.class).getSimpleName())
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
