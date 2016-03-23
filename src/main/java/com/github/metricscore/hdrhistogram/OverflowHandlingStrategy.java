package com.github.metricscore.hdrhistogram;

import org.HdrHistogram.Recorder;

public enum OverflowHandlingStrategy {

    SKIP {
        @Override
        public void write(long highestTrackableValue, long value, Recorder histogram) {
            // do nothing
        }
    },

    PASS_THRU {
        @Override
        public void write(long highestTrackableValue, long value, Recorder histogram) {
            histogram.recordValue(highestTrackableValue);
        }
    },

    REDUCE_TO_MAXIMUM {
        @Override
        public void write(long highestTrackableValue, long value, Recorder histogram) {
            histogram.recordValue(highestTrackableValue);
        }
    };
    
    public abstract void write(long highestTrackableValue, long value, Recorder histogram);

}
