package com.github.metricscore.hdrhistogram;

import org.HdrHistogram.Recorder;

import java.time.Duration;

public interface AccumulationStrategy {

    Accumulator createAccumulator(Recorder recorder);

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
        return new ResetPeriodicallyAccumulationStrategy(resettingPeriod, WallClock.INSTANCE);
    }

}
