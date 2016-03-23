package com.github.metricscore.hdrhistogram;

import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Snapshot;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;

import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A {@link com.codahale.metrics.Reservoir} implementation backed by a window that stores only the measurements made
 * since the last lastSnapshot was taken.
 */
public class HdrReservoir implements Reservoir {

    private final Lock lock = new ReentrantLock();
    private final Recorder recorder;
    private final long highestTrackableValue;
    private final OverflowHandlingStrategy overflowHandlingStrategy;
    private final SnapshotAccumulator snapshotAccumulator;
    private final long cachingDurationMillis;
    private final Optional<double[]> predefinedPercentiles;
    private final WallClock wallClock;

    private Histogram intervalHistogram;

    private HdrSnapshot cachedSnapshot;
    private long lastSnapshotTakeTimeMillis;

    HdrReservoir(int numberOfSignificantValueDigits,
                 Optional<Long> lowestDiscernibleValue,
                 Optional<Long> highestTrackableValue,
                 Optional<OverflowHandlingStrategy> overflowHandling,
                 SnapshotAccumulationStrategy snapshotAccumulationStrategy,
                 Optional<Long> cachingDurationMillis,
                 Optional<double[]> predefinedPercentiles,
                 WallClock wallClock
    ) {
        if (highestTrackableValue.isPresent() && lowestDiscernibleValue.isPresent()) {
            this.recorder = new Recorder(highestTrackableValue.get(), numberOfSignificantValueDigits);
            this.highestTrackableValue = highestTrackableValue.get();
            this.overflowHandlingStrategy = overflowHandling.get();
        } else if (highestTrackableValue.isPresent()) {
            this.recorder = new Recorder(highestTrackableValue.get(), numberOfSignificantValueDigits);
            this.highestTrackableValue = highestTrackableValue.get();
            this.overflowHandlingStrategy = overflowHandling.get();
        } else {
            this.recorder = new Recorder(numberOfSignificantValueDigits);
            this.highestTrackableValue = Long.MAX_VALUE;
            this.overflowHandlingStrategy = null;
        }
        this.predefinedPercentiles = predefinedPercentiles;

        this.cachingDurationMillis = cachingDurationMillis.orElse(0L);
        this.intervalHistogram = recorder.getIntervalHistogram();
        this.snapshotAccumulator = SnapshotAccumulator.create(snapshotAccumulationStrategy, intervalHistogram);
        this.wallClock = wallClock;
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException("You should not use this method https://github.com/dropwizard/metrics/issues/874");
    }

    @Override
    public void update(long value) {
        if (value > highestTrackableValue) {
            overflowHandlingStrategy.write(highestTrackableValue, value, recorder);
            return;
        }
        recorder.recordValue(value);
    }

    @Override
    public Snapshot getSnapshot() {
        lock.lock();
        try {
            intervalHistogram = recorder.getIntervalHistogram(intervalHistogram);
            new HdrSnapshot(intervalHistogram.copy());
            Histogram histogramForSnapshot = snapshotAccumulator.getHistogramForSnapshot(intervalHistogram);
            HdrSnapshot snapshot = HdrSnapshot.create(predefinedPercentiles, histogramForSnapshot);
            return snapshot;
        } finally {
            lock.unlock();
        }
    }

}
