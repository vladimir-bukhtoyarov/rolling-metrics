package com.github.metricscore.hdrhistogram;

import org.HdrHistogram.Histogram;

public interface Accumulator {

    Histogram rememberIntervalAndGetHistogramToTakeSnapshot(Histogram intervalHistogram);

}
