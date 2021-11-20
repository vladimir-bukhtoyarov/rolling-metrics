
/*
 *    Copyright 2017 Vladimir Bukhtoyarov
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package com.github.rollingmetrics.histogram.hdr.impl;

import com.github.rollingmetrics.histogram.OverflowResolver;
import com.github.rollingmetrics.histogram.hdr.RollingHdrHistogramBuilder;
import com.github.rollingmetrics.histogram.hdr.RollingHdrHistogram;
import com.github.rollingmetrics.histogram.hdr.RecorderSettings;
import com.github.rollingmetrics.histogram.hdr.RollingSnapshot;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.HistogramIterationValue;

import java.io.*;
import java.util.Arrays;
import java.util.function.Function;


/**
 * This class is not the part of rolling-metrics public API and should not be used by user directly.
 *
 * @see RollingHdrHistogramBuilder
 */
public abstract class AbstractRollingHdrHistogram implements RollingHdrHistogram {

    private final Function<Histogram, RollingSnapshot> snapshotTaker;
    private final long highestTrackableValue;
    private final OverflowResolver overflowResolver;
    private final long expectedIntervalBetweenValueSamples;

    protected AbstractRollingHdrHistogram(RecorderSettings recorderSettings) {
        this.highestTrackableValue = recorderSettings.getHighestTrackableValue().orElse(Long.MAX_VALUE);
        this.overflowResolver = recorderSettings.getOverflowResolver().orElse(null);
        this.expectedIntervalBetweenValueSamples = recorderSettings.getExpectedIntervalBetweenValueSamples().orElse(0L);

        if (recorderSettings.getPredefinedPercentiles().isPresent()) {
            double[] percentiles = recorderSettings.getPredefinedPercentiles().get();
            snapshotTaker = histogram -> takeSmartSnapshot(percentiles, histogram);
        } else {
            snapshotTaker = AbstractRollingHdrHistogram::takeFullSnapshot;
        }
    }

    @Override
    public RollingSnapshot getSnapshot() {
        return getSnapshot(snapshotTaker);
    }

    @Override
    public void update(long value) {
        if (value > highestTrackableValue) {
            switch (overflowResolver) {
                case SKIP: return;
                case PASS_THRU: break;
                case REDUCE_TO_HIGHEST_TRACKABLE: value = highestTrackableValue;
            }
        }
        recordSingleValueWithExpectedInterval(value, expectedIntervalBetweenValueSamples);
    }

    protected abstract RollingSnapshot getSnapshot(Function<Histogram, RollingSnapshot> snapshotTaker);

    protected abstract void recordSingleValueWithExpectedInterval(long value, long expectedIntervalBetweenValueSamples);

    static RollingSnapshot takeSmartSnapshot(final double[] predefinedQuantiles, Histogram histogram) {
        final long max = histogram.getMaxValue();
        final long min = histogram.getMinValue();
        final double mean = histogram.getMean();
        final double median = histogram.getValueAtPercentile(50.0);
        final double stdDeviation = histogram.getStdDeviation();
        final long samplesCount = histogram.getTotalCount();

        final double[] values = new double[predefinedQuantiles.length];
        for (int i = 0; i < predefinedQuantiles.length; i++) {
            double quantile = predefinedQuantiles[i];
            double percentile = quantile * 100.0;
            values[i] = histogram.getValueAtPercentile(percentile);
        }

        return createSmartSnapshot(predefinedQuantiles, max, min, mean, median, stdDeviation, values, samplesCount);
    }

    static RollingSnapshot createSmartSnapshot(final double[] predefinedQuantiles, final long max, final long min, final double mean,
                                               final double median, final double stdDeviation, final double[] values, final long samplesCount) {
        return new RollingSnapshot() {
            @Override
            public double getValue(double quantile) {
                for (int i = 0; i < predefinedQuantiles.length; i++) {
                    if (quantile <= predefinedQuantiles[i]) {
                        return values[i];
                    }
                }
                return max;
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
            public long getSamplesCount(){
                return samplesCount;
            }

            @Override
            public String toString() {
                StringBuilder distribution = new StringBuilder();
                for(int i = 0; i < predefinedQuantiles.length; i++) {
                    distribution.append(predefinedQuantiles[i] * 100).append("%:").append(values[i]).append("; ");
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

    private static RollingSnapshot takeFullSnapshot(final Histogram histogram) {
        return new RollingSnapshot() {
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
            public double getMedian() {
                return histogram.getValueAtPercentile(50.0);
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
            public long getSamplesCount(){
                return histogram.getTotalCount();
            }

            @Override
            public String toString() {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                PrintStream printStream;
                printStream = new PrintStream(stream, true);
                histogram.outputPercentileDistribution(printStream, 1.0);
                String distributionAsString = new String(stream.toByteArray());
                return "FullSnapshot{" + distributionAsString + "}";
            }
        };
    }

    @Override
    public String toString() {
        return "AbstractRollingHdrHistogram{" +
                "highestTrackableValue=" + highestTrackableValue +
                ", overflowResolver=" + overflowResolver +
                ", expectedIntervalBetweenValueSamples=" + expectedIntervalBetweenValueSamples +
                '}';
    }

    public static String histogramValuesToString(Histogram histogram) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PrintStream writer = new PrintStream(baos);
            histogram.outputPercentileDistribution(writer, 1.0);
            byte[] resultBytes = baos.toByteArray();
            return new String(resultBytes);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

}
