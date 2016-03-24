package com.github.metricscore.hdrhistogram;

import org.HdrHistogram.Recorder;

import java.time.Duration;

public interface AccumulationStrategy {

    Accumulator createAccumulator(Recorder initialHistogram);

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
     * Reservoir configured with this strategy will be cleared each {@code resetPeriod} time.
     *
     * @param resetPeriod specifies how often need to reset reservoir
     */
    static AccumulationStrategy resetPeriodically(Duration resetPeriod) {
        return new ResetPeriodicallyAccumulationStrategy(resetPeriod, WallClock.INSTANCE);
    }

}
