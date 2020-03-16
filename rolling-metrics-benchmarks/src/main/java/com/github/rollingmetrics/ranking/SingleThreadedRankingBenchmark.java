/*
 *
 *  Copyright 2020 Vladimir Bukhtoyarov
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

package com.github.rollingmetrics.ranking;

import com.github.rollingmetrics.ranking.impl.recorder.SingleThreadedRanking;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class SingleThreadedRankingBenchmark {

    @State(Scope.Thread)
    public static class SingleThreadedState {

        private static final int TEST_DATA_COUNT = 1024 * 32;

        final String[] identities = new String[TEST_DATA_COUNT];

        final SingleThreadedRanking singleThreadedRanking = new SingleThreadedRanking(10, 0);

        @Setup
        public void setup() {
            for (int i = 0; i < TEST_DATA_COUNT; i++) {
                identities[i] = i + "";
            }
        }

    }

    @Group("singleThreadedRanking_update")
    @GroupThreads(1)
    @Benchmark
    public void singleThreadedRankingUpdate(SingleThreadedState state) {
        state.singleThreadedRanking.update(getRandomValue(), state.identities[getRandomValue()]);
    }

    public static class OneThread {
        public static void main(String[] args) throws RunnerException {
            Options opt = new OptionsBuilder()
                    .include(".*" + SingleThreadedRankingBenchmark.class.getSimpleName() + ".*")
                    .warmupIterations(3)
                    .warmupTime(TimeValue.seconds(2))
                    .measurementIterations(3)
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

    private static int getRandomValue() {
        return ThreadLocalRandom.current().nextInt(SingleThreadedState.TEST_DATA_COUNT);
    }

}
