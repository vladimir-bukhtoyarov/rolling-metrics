package com.github.addon.metrics.reservoir.hdr;

import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Snapshot;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A {@link com.codahale.metrics.Reservoir} implementation backed by a window that stores only the measurements made
 * since the last snapshot was taken.
 */
public class ResetOnSnapshotWindowHdrReservoir implements Reservoir {

    private final Lock lock = new ReentrantLock();
    private final Recorder recorder;

    // holds the data since the last snapshot was taken
    private Histogram intervalHistogram;

    public ResetOnSnapshotWindowHdrReservoir(Recorder recorder) {
        this.recorder = recorder;
        intervalHistogram = recorder.getIntervalHistogram();
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
            return new HdrSnapshot(intervalHistogram.copy());
        } finally {
            lock.unlock();
        }
    }

}
