package com.github.rollingmetrics.histogram.hdr.impl;

import com.github.rollingmetrics.histogram.OverflowResolver;
import com.github.rollingmetrics.histogram.hdr.RollingHdrHistogram;
import com.github.rollingmetrics.histogram.hdr.RollingHdrHistogramBuilder;
import com.github.rollingmetrics.retention.*;
import com.github.rollingmetrics.util.ResilientExecutionUtil;
import com.github.rollingmetrics.util.Ticker;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

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
    public RollingHdrHistogramBuilder withSnapshotCachingDuration(Duration duration) {
        if (duration.isNegative()) {
            throw new IllegalArgumentException(duration + " is negative");
        }
        if (duration.isZero()) {
            this.snapshotCachingDuration = Optional.empty();
        } else {
            this.snapshotCachingDuration = Optional.of(duration);
        }
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
    public RollingHdrHistogramBuilder withBackgroundExecutor(Executor backgroundExecutor) {
        recorderSettings.setBackgroundExecutor(backgroundExecutor);
        return this;
    }

    /**
     * TODO
     *
     * @param ticker
     * @return
     */
    public RollingHdrHistogramBuilder withTicker(Ticker ticker) {
        this.ticker = Objects.requireNonNull(ticker);
        return this;
    }

    /**
     * TODO
     *
     * @return
     */
    public RollingHdrHistogram build() {
        recorderSettings.validateParameters();
        RollingHdrHistogram rollingHdrHistogram = builders.get(retentionPolicy.getClass()).create(recorderSettings, retentionPolicy, ticker);
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
                ", snapshotCachingDurationMillis=" + snapshotCachingDuration +
                ", recorderSettings=" + recorderSettings +
                '}';
    }

    private final RetentionPolicy retentionPolicy;
    private RecorderSettings recorderSettings;
    private Optional<Duration> snapshotCachingDuration;
    private Ticker ticker;

    DefaultRollingHdrHistogramBuilder(RetentionPolicy retentionPolicy) {
        if (!builders.containsKey(retentionPolicy.getClass())) {
            throw new IllegalArgumentException("Unknown retention policy " + retentionPolicy.getClass());
        }
        this.retentionPolicy =  retentionPolicy;
        this.ticker = Ticker.defaultTicker();
        this.snapshotCachingDuration = Optional.empty();
        this.recorderSettings = new RecorderSettings();
    }

    private RollingHdrHistogram wrapAroundByDecorators(RollingHdrHistogram histogram) {
        // wrap around by decorator if snapshotCachingDurationMillis was specified
        if (snapshotCachingDuration.isPresent()) {
            histogram = new SnapshotCachingRollingHdrHistogram(histogram, snapshotCachingDuration.get(), ticker);
        }
        return histogram;
    }

    private static Map<Class<? extends RetentionPolicy>, HistogramFactory> builders = new HashMap<>();
    static {
        builders.put(UniformRetentionPolicy.class, (settings, retentionPolicy, ticker) -> new UniformRollingHdrHistogramImpl(settings));
        builders.put(ResetOnSnapshotRetentionPolicy.class, (settings, retentionPolicy, ticker) -> new ResetOnSnapshotRollingHdrHistogramImpl(settings));
        builders.put(ResetPeriodicallyRetentionPolicy.class, (settings, retentionPolicy, ticker) ->
                new ResetByChunksRollingHdrHistogramImpl(settings, (ResetPeriodicallyRetentionPolicy) retentionPolicy, ticker));
        builders.put(ResetPeriodicallyByChunksRetentionPolicy.class, (settings, retentionPolicy, ticker) ->
                new ResetByChunksRollingHdrHistogramImpl(settings, (ResetPeriodicallyByChunksRetentionPolicy) retentionPolicy, ticker));
    }

    private interface HistogramFactory {

        RollingHdrHistogram create(RecorderSettings settings, RetentionPolicy retentionPolicy, Ticker ticker);

    }

}
