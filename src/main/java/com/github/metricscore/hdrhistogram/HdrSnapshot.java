package com.github.metricscore.hdrhistogram;

import com.codahale.metrics.Snapshot;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.HistogramIterationValue;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Arrays;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;

public class HdrSnapshot {

    public static Snapshot create(Optional<double[]> predefinedPercentiles, Histogram histogramForSnapshot) {
        if (predefinedPercentiles.isPresent()) {
            return smartSnapshot(predefinedPercentiles.get(), histogramForSnapshot);
        } else {
            return fullSnapshot(histogramForSnapshot.copy());
        }
    }

    static Snapshot smartSnapshot(final double[] predefinedPercentiles, final Histogram histogram) {
        return new Snapshot() {
            final long max = histogram.getMaxValue();
            final long min = histogram.getMinValue();
            final double mean = histogram.getMean();
            final double stdDeviation = histogram.getStdDeviation();
            final double[] values = new double[predefinedPercentiles.length];

            @Override
            public double getValue(double quantile) {
                return 0;
            }

            @Override
            public long[] getValues() {
                long[] toReturn = new long[values.length];
                for (int i = 0; i < values.length; i++) {
                    toReturn[i] = (long) values[i];
                }
                return toReturn;
            }

            @Override
            public int size() {
                return values.length;
            }

            @Override
            public long getMax() {
                return max;
            }

            @Override
            public double getMean() {
                return mean;
            }

            @Override
            public long getMin() {
                return min;
            }

            @Override
            public double getStdDev() {
                return stdDeviation;
            }

            @Override
            public void dump(OutputStream output) {
                try (PrintWriter p = new PrintWriter(new OutputStreamWriter(output, UTF_8))) {
                    for (double value : values) {
                        p.printf("%d%n", value);
                    }
                }
            }

            @Override
            public String toString() {
                return "SmartSnapshot{" +
                        "max=" + max +
                        ", min=" + min +
                        ", mean=" + mean +
                        ", stdDeviation=" + stdDeviation +
                        ", values=" + Arrays.toString(values) +
                        '}';
            }
        };
    }

    static Snapshot fullSnapshot(final Histogram histogram) {
        return new Snapshot() {
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

            @Override
            public String toString() {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                PrintStream printStream = null;
                try {
                    printStream = new PrintStream(true, stream, Charset.defaultCharset());
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
                histogram.outputPercentileDistribution(printStream, 1.0);
                String distribution = new String(stream.toByteArray());
                return "FullSnapshot{" + distribution + "}";
            }
        };
    }

}
