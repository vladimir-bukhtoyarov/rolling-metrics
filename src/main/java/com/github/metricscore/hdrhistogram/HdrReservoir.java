package com.github.metricscore.hdrhistogram;

import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Snapshot;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.HistogramIterationValue;
import org.HdrHistogram.Recorder;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A {@link com.codahale.metrics.Reservoir} implementation backed by {@link org.HdrHistogram.Recorder}
 */
public class HdrReservoir implements Reservoir {

    private final long highestTrackableValue;
    private final OverflowHandlingStrategy overflowHandlingStrategy;
    private final Accumulator accumulator;
    private final Function<Histogram, Snapshot> snapshotTaker;

    HdrReservoir(
             WallClock wallClock,
             AccumulationStrategy accumulationStrategy,
             int numberOfSignificantValueDigits,
             Optional<Long> lowestDiscernibleValue,
             Optional<Long> highestTrackableValue,
             Optional<OverflowHandlingStrategy> overflowHandling,
             Optional<double[]> predefinedPercentiles
    ) {
        Recorder recorder;
        if (highestTrackableValue.isPresent() && lowestDiscernibleValue.isPresent()) {
            recorder = new Recorder(lowestDiscernibleValue.get(), highestTrackableValue.get(), numberOfSignificantValueDigits);
            this.highestTrackableValue = highestTrackableValue.get();
            this.overflowHandlingStrategy = overflowHandling.get();
        } else if (highestTrackableValue.isPresent()) {
            recorder = new Recorder(highestTrackableValue.get(), numberOfSignificantValueDigits);
            this.highestTrackableValue = highestTrackableValue.get();
            this.overflowHandlingStrategy = overflowHandling.get();
        } else {
            recorder = new Recorder(numberOfSignificantValueDigits);
            this.highestTrackableValue = Long.MAX_VALUE;
            this.overflowHandlingStrategy = null;
        }
        this.accumulator = accumulationStrategy.createAccumulator(recorder, wallClock);

        if (predefinedPercentiles.isPresent()) {
            double[] percentiles = predefinedPercentiles.get();
            snapshotTaker = histogram -> takeSmartSnapshot(percentiles, histogram);
        } else {
            snapshotTaker = HdrReservoir::takeFullSnapshot;
        }
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException("You should not use this method https://github.com/dropwizard/metrics/issues/874");
    }

    @Override
    public void update(long value) {
        if (value > highestTrackableValue) {
            switch (overflowHandlingStrategy) {
                case SKIP: break;
                case PASS_THRU: accumulator.recordValue(value); break;
                case REDUCE_TO_MAXIMUM: accumulator.recordValue(highestTrackableValue); break;
                default: throw new IllegalStateException("42");
            }
        } else {
            accumulator.recordValue(value);
        }
    }

    @Override
    public Snapshot getSnapshot() {
        return accumulator.getSnapshot(snapshotTaker);
    }

    private static Snapshot takeSmartSnapshot(final double[] predefinedQuantiles, Histogram histogram) {
        final long max = histogram.getMaxValue();
        final long min = histogram.getMinValue();
        final double mean = histogram.getMean();
        final double median = histogram.getValueAtPercentile(50.0);
        final double stdDeviation = histogram.getStdDeviation();

        final double[] values = new double[predefinedQuantiles.length];
        for (int i = 0; i < predefinedQuantiles.length; i++) {
            double quantile = predefinedQuantiles[i];
            double percentile = quantile * 100.0;
            values[i] = histogram.getValueAtPercentile(percentile);
        }

        return createSmartSnapshot(predefinedQuantiles, max, min, mean, median, stdDeviation, values);
    }

    private static Snapshot createSmartSnapshot(final double[] predefinedQuantiles, final long max, final long min, final double mean, final double median, final double stdDeviation, final double[] values) {
        return new Snapshot() {
            @Override
            public double getValue(double quantile) {
                if (quantile <= predefinedQuantiles[0]) {
                    return values[0];
                }
                if (quantile > predefinedQuantiles[predefinedQuantiles.length - 1]) {
                    return max;
                }
                for (int i = 1; i < predefinedQuantiles.length; i++) {
                    if (quantile <= predefinedQuantiles[i]) {
                        return values[i];
                    }
                }
                throw new IllegalStateException("42");
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

            public double getMedian() {
                return median;
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
                StringBuilder distribution = new StringBuilder();
                for(int i = 0; i < predefinedQuantiles.length; i++) {
                    distribution.append(predefinedQuantiles[i] * 100).append("% - ").append(values[i]).append(";");
                }
                return "SmartSnapshot{" +
                        "max=" + max +
                        ", min=" + min +
                        ", mean=" + mean +
                        ", stdDeviation=" + stdDeviation +
                        ", distribution=" + distribution +
                        '}';
            }
        };
    }

    private static Snapshot takeFullSnapshot(final Histogram histogram) {
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
                PrintStream printStream;
                try {
                    printStream = new PrintStream(stream, true, Charset.defaultCharset().name());
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
                histogram.outputPercentileDistribution(printStream, 1.0);
                String distributionAsString = new String(stream.toByteArray());
                return "FullSnapshot{" + distributionAsString + "}";
            }
        };
    }

}
