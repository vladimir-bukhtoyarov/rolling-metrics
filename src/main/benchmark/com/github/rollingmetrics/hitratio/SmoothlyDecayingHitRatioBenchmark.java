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

package com.github.rollingmetrics.hitratio;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class SmoothlyDecayingHitRatioBenchmark {

    @org.openjdk.jmh.annotations.State(Scope.Benchmark)
    public static class State {
        public final HitRatio hitRatio = new SmoothlyDecayingRollingHitRatio(Duration.ofSeconds(1), 100);
    }

    @Benchmark
    @Group("readSumWithContendedWrite")
    @GroupThreads(3)
    public void update(State state) {
        state.hitRatio.incrementHitCount();
    }

    @Benchmark
    @Group("readSumWithContendedWrite")
    @GroupThreads(1)
    public double readRatio(State state) {
        return state.hitRatio.getHitRatio();
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(SmoothlyDecayingHitRatioBenchmark.class.getSimpleName())
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
