package com.github.metricscore.hdrhistogram;

import org.HdrHistogram.Histogram;

public interface AccumulationStrategy {

    Accumulator createAccumulator(Histogram initialHistogram);

    /**
     * Reservoir configured with this strategy will be cleared each time when snapshot taken.
     * This is default strategy for {@link HdrBuilder}
     */
    AccumulationStrategy RESET_ON_SNAPSHOT = new AccumulationStrategy() {
        private final Accumulator INSTANCE = new Accumulator() {
            @Override
            public Histogram rememberIntervalAndGetHistogramToTakeSnapshot(Histogram intervalHistogram) {
                return intervalHistogram;
            }
        };
        @Override
        public Accumulator createAccumulator(Histogram initialHistogram) {
            return INSTANCE;
        }
    };

    /**
     * Reservoir configured with this strategy will store all measures since the reservoir was created.
     */
    AccumulationStrategy UNIFORM = new AccumulationStrategy() {
        @Override
        public Accumulator createAccumulator(Histogram initialHistogram) {
            final Histogram uniformHistogram = initialHistogram.copy();
            return new Accumulator() {
                @Override
                public Histogram rememberIntervalAndGetHistogramToTakeSnapshot(Histogram intervalHistogram) {
                    uniformHistogram.add(intervalHistogram);
                    return uniformHistogram;
                }
            };
        }
    };

}
