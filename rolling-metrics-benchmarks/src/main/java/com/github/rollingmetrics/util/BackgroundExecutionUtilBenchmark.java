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

package com.github.rollingmetrics.util;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class BackgroundExecutionUtilBenchmark {

    @org.openjdk.jmh.annotations.State(Scope.Benchmark)
    public static class State {
        public final Executor executor = ResilientExecutionUtil.getInstance().getBackgroundExecutor();
        public final Executor jdkExecutor = new ThreadPoolExecutor(1, 1,
                                      0L,TimeUnit.MILLISECONDS,
                                      new LinkedBlockingQueue<>(),
                                      new DaemonThreadFactory(""));
    }

    @Benchmark
    public void baseLine() {
        Blackhole.consumeCPU(1000);
    }

    @Benchmark
    public void costOfScheduling(State state) {
        state.executor.execute(() -> {});
        Blackhole.consumeCPU(1000);
    }

    @Benchmark
    public void costOfSchedulingOnJdkExecutor(State state) {
        state.jdkExecutor.execute(() -> {});
        Blackhole.consumeCPU(1000);
    }

    @Benchmark
    public void fullCycle(State state) {
        AtomicBoolean executed = new AtomicBoolean(false);
        state.executor.execute(() -> executed.set(true));
        while (!executed.get());
    }

    @Benchmark
    public void fullCycleOnJdkExecutor(State state) {
        AtomicBoolean executed = new AtomicBoolean(false);
        state.jdkExecutor.execute(() -> executed.set(true));
        while (!executed.get());
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(BackgroundExecutionUtilBenchmark.class.getSimpleName())
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
