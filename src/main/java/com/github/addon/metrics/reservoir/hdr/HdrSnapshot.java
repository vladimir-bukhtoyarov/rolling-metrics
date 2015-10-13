package com.github.addon.metrics.reservoir.hdr;

import com.codahale.metrics.Snapshot;
import org.HdrHistogram.Histogram;

import java.io.OutputStream;

public class HdrSnapshot extends Snapshot {

    private final Histogram histogram;

    public HdrSnapshot(Histogram histogram) {
        this.histogram = histogram;
    }

    @Override
    public double getValue(double quantile) {
        return histogram.getValueAtPercentile(quantile);
    }

    @Override
    public long[] getValues() {
        // return something
        return new long[0];
    }

    @Override
    public int size() {
        return (int) histogram.getTotalCount();
    }

    @Override
    public long getMax() {
        return histogram.getMaxValue();
    }

    @Override
    public double getMean() {
        return histogram.getMean();
    }

    @Override
    public long getMin() {
        return histogram.getMinValue();
    }

    @Override
    public double getStdDev() {
        return histogram.getStdDeviation();
    }

    @Override
    public void dump(OutputStream output) {
        // do nothing
    }

}
