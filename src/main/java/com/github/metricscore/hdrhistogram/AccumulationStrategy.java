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

package com.github.metricscore.hdrhistogram;

import org.HdrHistogram.Recorder;

import java.time.Duration;

public interface AccumulationStrategy {

    Accumulator createAccumulator(Recorder recorder, WallClock wallClock);

    /**
     * Reservoir configured with this strategy will be cleared each time when snapshot taken.
     * This is default strategy for {@link HdrBuilder}
     */
    static AccumulationStrategy resetOnSnapshot() {
        return ResetOnSnapshotAccumulationStrategy.INSTANCE;
    }

    /**
     * Reservoir configured with this strategy will store all measures since the reservoir was created.
     */
    static AccumulationStrategy uniform() {
       return UniformAccumulationStrategy.INSTANCE;
    }

    /**
     * Reservoir configured with this strategy will be cleared after each {@code resettingPeriod} gone.
     *
     * @param resettingPeriod specifies how often need to reset reservoir
     */
    static AccumulationStrategy resetPeriodically(Duration resettingPeriod) {
        return new ResetPeriodicallyAccumulationStrategy(resettingPeriod);
    }

}
