package com.github.addon.metrics.reservoir.hdr;

import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Snapshot;
import org.HdrHistogram.Histogram;

public class UniformHdrReservoir implements Reservoir {

    private final Histogram histogram;

    public UniformHdrReservoir(Histogram histogram) {
        this.histogram = histogram;
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException("You should not use this method");
    }

    @Override
    public void update(long value) {
        histogram.recordValue(value);
    }

    @Override
    public Snapshot getSnapshot() {
        return new HdrSnapshot(histogram.copy());
    }

}
