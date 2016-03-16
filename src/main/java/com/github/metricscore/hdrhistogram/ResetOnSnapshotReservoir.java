package com.github.metricscore.hdrhistogram;

import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Snapshot;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;

import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A {@link com.codahale.metrics.Reservoir} implementation backed by a window that stores only the measurements made
 * since the last lastSnapshot was taken.
 */
public class ResetOnSnapshotReservoir implements Reservoir {

    private final Lock lock = new ReentrantLock();
    private final Recorder recorder;
    private Histogram intervalHistogram;

    public ResetOnSnapshotReservoir(Recorder recorder) {
        this.recorder = Objects.requireNonNull(recorder);
        this.intervalHistogram = recorder.getIntervalHistogram();
    }

    // unnecessary method https://github.com/dropwizard/metrics/issues/874
    @Override
    public int size() {
        // I hope "size" is called infrequently
        return getSnapshot().size();
    }

    @Override
    public void update(long value) {
        recorder.recordValue(value);
    }

    @Override
    public Snapshot getSnapshot() {
        lock.lock();
        try {
            intervalHistogram = recorder.getIntervalHistogram(intervalHistogram);
            HdrSnapshot snapshot = new HdrSnapshot(intervalHistogram.copy());
            return snapshot;
        } finally {
            lock.unlock();
        }
    }

}
