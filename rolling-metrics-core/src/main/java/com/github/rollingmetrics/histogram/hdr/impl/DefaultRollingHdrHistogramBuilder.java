package com.github.rollingmetrics.histogram.hdr.impl;

import com.github.rollingmetrics.histogram.OverflowResolver;
import com.github.rollingmetrics.histogram.hdr.RollingHdrHistogram;
import com.github.rollingmetrics.histogram.hdr.RollingHdrHistogramBuilder;
import com.github.rollingmetrics.retention.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation of {@link RollingHdrHistogramBuilder}.
 */
public class DefaultRollingHdrHistogramBuilder implements RollingHdrHistogramBuilder {

    @Override
    public RollingHdrHistogramBuilder withSignificantDigits(int numberOfSignificantValueDigits) {
        recorderSettings.setSignificantDigits(numberOfSignificantValueDigits);
        return this;
    }

    @Override
    public RollingHdrHistogramBuilder withLowestDiscernibleValue(long lowestDiscernibleValue) {
        recorderSettings.setLowestDiscernibleValue(lowestDiscernibleValue);
        return this;
    }

    @Override
    public RollingHdrHistogramBuilder withHighestTrackableValue(long highestTrackableValue, OverflowResolver overflowResolver) {
        recorderSettings.setHighestTrackableValue(highestTrackableValue, overflowResolver);
        return this;
    }

    @Override
    public RollingHdrHistogramBuilder withExpectedIntervalBetweenValueSamples(long expectedIntervalBetweenValueSamples) {
        recorderSettings.setExpectedIntervalBetweenValueSamples(expectedIntervalBetweenValueSamples);
        return this;
    }

    @Override
    public RollingHdrHistogramBuilder withPredefinedPercentiles(double[] predefinedPercentiles) {
        recorderSettings.setPredefinedPercentiles(predefinedPercentiles);
        return this;
    }

    @Override
    public RollingHdrHistogramBuilder withoutSnapshotOptimization() {
        recorderSettings.withoutSnapshotOptimization();
        return this;
    }

    @Override
    public RollingHdrHistogram build() {
        recorderSettings.validateParameters();
        RollingHdrHistogram rollingHdrHistogram = builders.get(retentionPolicy.getClass()).create(recorderSettings, retentionPolicy);
        return wrapAroundByDecorators(rollingHdrHistogram);
    }

    @Override
    public int getEstimatedFootprintInBytes() {
        return build().getEstimatedFootprintInBytes();
    }

    @Override
    public String toString() {
        return "RollingHdrHistogramBuilder{" +
                "retentionPolicy=" + retentionPolicy +
                ", recorderSettings=" + recorderSettings +
                '}';
    }

    private final RetentionPolicy retentionPolicy;
    private RecorderSettings recorderSettings;

    public DefaultRollingHdrHistogramBuilder(RetentionPolicy retentionPolicy) {
        if (!builders.containsKey(retentionPolicy.getClass())) {
            throw new IllegalArgumentException("Unknown retention policy " + retentionPolicy.getClass());
        }
        this.retentionPolicy =  retentionPolicy;
        this.recorderSettings = new RecorderSettings();
    }

    private RollingHdrHistogram wrapAroundByDecorators(RollingHdrHistogram histogram) {
        // wrap around by decorator if snapshotCachingDurationMillis was specified
        if (!retentionPolicy.getSnapshotCachingDuration().isZero()) {
            histogram = new SnapshotCachingRollingHdrHistogram(histogram, retentionPolicy.getSnapshotCachingDuration(), retentionPolicy.getTicker());
        }
        return histogram;
    }

    private static Map<Class<? extends RetentionPolicy>, HistogramFactory> builders = new HashMap<>();
    static {
        builders.put(UniformRetentionPolicy.class, (settings, retentionPolicy) -> new UniformRollingHdrHistogramImpl(settings));
        builders.put(ResetOnSnapshotRetentionPolicy.class, (settings, retentionPolicy) -> new ResetOnSnapshotRollingHdrHistogramImpl(settings));
        builders.put(ResetPeriodicallyRetentionPolicy.class, (settings, retentionPolicy) ->
                new ResetByChunksRollingHdrHistogramImpl(settings, (ResetPeriodicallyRetentionPolicy) retentionPolicy));
        builders.put(ResetPeriodicallyByChunksRetentionPolicy.class, (settings, retentionPolicy) ->
                new ResetByChunksRollingHdrHistogramImpl(settings, (ResetPeriodicallyByChunksRetentionPolicy) retentionPolicy));
    }

    private interface HistogramFactory {

        RollingHdrHistogram create(RecorderSettings settings, RetentionPolicy retentionPolicy);

    }

}
