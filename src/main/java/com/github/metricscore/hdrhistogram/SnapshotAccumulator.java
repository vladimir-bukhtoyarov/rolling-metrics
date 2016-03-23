package com.github.metricscore.hdrhistogram;

import org.HdrHistogram.Histogram;

public abstract class SnapshotAccumulator {

    private static SnapshotAccumulator RESET_ON_SNAPSHOT_INSTANCE = new SnapshotAccumulator() {
        @Override
        public Histogram getHistogramForSnapshot(Histogram intervalHistogram) {
            return intervalHistogram;
        }
    };

    static SnapshotAccumulator create(SnapshotAccumulationStrategy strategy, Histogram intervalHistogram) {
        switch (strategy) {
            case RESET_ON_SNAPSHOT: return createResetOnSnapshot(intervalHistogram);
            case UNIFORM: return createResetOnSnapshot(intervalHistogram);
        }
    }

    public abstract Histogram getHistogramForSnapshot(Histogram intervalHistogram);

}
