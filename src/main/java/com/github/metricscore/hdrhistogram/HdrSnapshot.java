package com.github.metricscore.hdrhistogram;

import com.codahale.metrics.Snapshot;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.HistogramIterationValue;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Arrays;

import static java.nio.charset.StandardCharsets.UTF_8;

public class HdrSnapshot extends Snapshot {

    private final Histogram histogram;

    public HdrSnapshot(Histogram histogram) {
        this.histogram = histogram;
    }

    @Override
    public double getValue(double quantile) {
        double percentile = quantile * 100.0;
        return histogram.getValueAtPercentile(percentile);
    }

    @Override
    public long[] getValues() {
        long[] values = new long[1024];
        int i = 0;
        for (HistogramIterationValue value : histogram.recordedValues()) {
            values[i] = value.getValueIteratedTo();
            i++;
            if (i == values.length) {
                values = Arrays.copyOf(values, values.length * 2);
            }
        }
        return Arrays.copyOf(values, i);
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
        try (PrintWriter p = new PrintWriter(new OutputStreamWriter(output, UTF_8))) {
            for (HistogramIterationValue value : histogram.recordedValues()) {
                for (int j = 0; j < value.getCountAddedInThisIterationStep(); j++) {
                    p.printf("%d%n", value.getValueIteratedTo());
                }
            }
        }
    }

}
