package com.github.metricscore.hdrhistogram;

import com.codahale.metrics.Snapshot;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;

import java.util.concurrent.locks.Lock;
import java.util.function.Function;

public interface Accumulator {

    void recordValue(long value);

    Snapshot getSnapshot(Function<Histogram, Snapshot> snapshotTaker);

}
