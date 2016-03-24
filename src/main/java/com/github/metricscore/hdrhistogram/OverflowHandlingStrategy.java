package com.github.metricscore.hdrhistogram;

import org.HdrHistogram.Recorder;

public enum OverflowHandlingStrategy {

    SKIP,

    PASS_THRU,

    REDUCE_TO_MAXIMUM;

}
