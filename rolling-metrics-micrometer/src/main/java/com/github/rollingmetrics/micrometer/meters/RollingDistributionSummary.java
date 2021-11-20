package com.github.rollingmetrics.micrometer.meters;

import com.github.rollingmetrics.histogram.OverflowResolver;
import com.github.rollingmetrics.histogram.hdr.RollingHdrHistogram;
import com.github.rollingmetrics.histogram.hdr.RollingHdrHistogramBuilder;
import com.github.rollingmetrics.histogram.hdr.RollingSnapshot;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.distribution.CountAtBucket;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.instrument.distribution.HistogramSnapshot;
import io.micrometer.core.instrument.distribution.ValueAtPercentile;

public class RollingDistributionSummary implements DistributionSummary {
    private final RollingHdrHistogram rollingHdrHistogram;
    private final Id id;
    private final double scale;
    private final double[] percentiles;

    public RollingDistributionSummary(Meter.Id id, DistributionStatisticConfig config, double scale, Clock clock) {
        this.id = id;
        this.scale = scale;
        TickerClock tickerClock = new TickerClock(clock);
        if (config.getBufferLength() == null) {
            throw new IllegalArgumentException("distributionStatisticConfig.getBufferLength() should not be null");
        }


        if (config.getPercentiles() != null) {
            percentiles = config.getPercentiles();
        } else {
            percentiles = new double[]{0.5, 0.95, 0.99};
        }
        Integer percentilePrecision = config.getPercentilePrecision();
        if (percentilePrecision == null) {
            percentilePrecision = 1;
        }

        RollingHdrHistogramBuilder builder = RollingHdrHistogram.builder();

        builder.withSignificantDigits(percentilePrecision);
        builder.resetReservoirPeriodicallyByChunks(config.getExpiry(), config.getBufferLength());

        Double maximumExpectedValue = config.getMaximumExpectedValueAsDouble();
        if (maximumExpectedValue != null) {
            builder.withHighestTrackableValue(
                    maximumExpectedValue.longValue(),
                    OverflowResolver.REDUCE_TO_HIGHEST_TRACKABLE);
        }

        builder.withPredefinedPercentiles(percentiles);
        builder.withTicker(tickerClock);
        rollingHdrHistogram = builder.build();

    }

    @Override
    public void record(double v) {
        long value = (long) (v * scale);
        rollingHdrHistogram.update(value);
    }

    @Override
    public long count() {
        return rollingHdrHistogram.getSnapshot().getSamplesCount();
    }

    @Override
    public double totalAmount() {
        RollingSnapshot snapshot = rollingHdrHistogram.getSnapshot();
        return snapshot.getMean() * snapshot.size();
    }

    @Override
    public double max() {
        return rollingHdrHistogram.getSnapshot().getMax();
    }

    @Override
    public HistogramSnapshot takeSnapshot() {
        RollingSnapshot snapshot = rollingHdrHistogram.getSnapshot();
        ValueAtPercentile[] valuesAtPercentile = new ValueAtPercentile[percentiles.length];
        for (int i = 0; i < percentiles.length; i++) {
            double percentile = percentiles[i];
            double value = snapshot.getValue(percentile);
            valuesAtPercentile[i] = new ValueAtPercentile(percentile, value);
        }
        return new HistogramSnapshot(
                snapshot.getSamplesCount(),
                snapshot.getMean() * snapshot.getSamplesCount(),
                snapshot.getMax(),
                valuesAtPercentile,
                new CountAtBucket[0],
                (printStream, aDouble) -> {
                }
        );
    }

    @Override
    public Id getId() {
        return id;
    }
}
