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

package com.github.metricscore.hdr.counter;

import com.github.metricscore.hdr.RunnerUtil;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.RunnerException;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class SmoothlyDecayingRollingCounterBenchmark {

    @State(Scope.Benchmark)
    public static class CounterState {
        public final WindowCounter counter = new SmoothlyDecayingRollingCounter(Duration.ofSeconds(1), 10);
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
        RunnerUtil.runBenchmark(4, SmoothlyDecayingRollingCounterBenchmark.class);
    }

}
