package com.github.rollingmetrics.top.impl;

import com.github.rollingmetrics.retention.*;
import com.github.rollingmetrics.top.Top;
import com.github.rollingmetrics.top.TopBuilder;
import com.github.rollingmetrics.top.TopRecorderSettings;
import com.github.rollingmetrics.util.Ticker;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;

public class DefaultTopBuilder implements TopBuilder {

    private final RetentionPolicy retentionPolicy;
    private final TopRecorderSettings settings;

    private Duration snapshotCachingDuration = DEFAULT_SNAPSHOT_CACHING_DURATION;
    private Ticker ticker = Ticker.defaultTicker();

    /**
     * Creates new builder instance.
     *
     * @param size the count of positions for tops which will be constructed by this builder
     * @param retentionPolicy policy which responsible for decisions how long any written value should be remembered
     * @return this builder instance
     */
    public DefaultTopBuilder(int size, RetentionPolicy retentionPolicy) {
        this.retentionPolicy = Objects.requireNonNull(retentionPolicy);
        if (!builders.containsKey(retentionPolicy.getClass())) {
            throw new IllegalArgumentException("Unknown retention policy " + retentionPolicy.getClass());
        }
        this.settings = new TopRecorderSettings(size);
    }


    @Override
    public Top build() {
        Top top = builders.get(retentionPolicy.getClass()).create(settings, retentionPolicy, ticker);
        if (!snapshotCachingDuration.isZero()) {
            top = new SnapshotCachingTop(top, snapshotCachingDuration, ticker);
        }
        return top;
    }

    @Override
    public TopBuilder withPositionCount(int size) {
        settings.setSize(size);
        return this;
    }

    @Override
    public TopBuilder withLatencyThreshold(Duration latencyThreshold) {
        settings.setLatencyThreshold(latencyThreshold);
        return this;
    }

    @Override
    public TopBuilder withSnapshotCachingDuration(Duration snapshotCachingDuration) {
        if (snapshotCachingDuration == null) {
            throw new IllegalArgumentException("snapshotCachingDuration should not be null");
        }
        if (snapshotCachingDuration.isNegative()) {
            throw new IllegalArgumentException("snapshotCachingDuration can not be negative");
        }
        this.snapshotCachingDuration = snapshotCachingDuration;
        return this;
    }

    @Override
    public TopBuilder withMaxLengthOfQueryDescription(int maxLengthOfQueryDescription) {
        settings.setMaxLengthOfQueryDescription(maxLengthOfQueryDescription);
        return this;
    }

    @Override
    public TopBuilder withTicker(Ticker ticker) {
        if (ticker == null) {
            throw new IllegalArgumentException("Ticker should not be null");
        }
        this.ticker = ticker;
        return this;
    }

    @Override
    public TopBuilder withBackgroundExecutor(Executor backgroundExecutor) {
        settings.setBackgroundExecutor(backgroundExecutor);
        return this;
    }

    private static Map<Class<? extends RetentionPolicy>, TopFactory> builders = new HashMap<>();
    static {
        builders.put(UniformRetentionPolicy.class, (settings, retentionPolicy, ticker) -> new UniformTop(settings));
        builders.put(ResetOnSnapshotRetentionPolicy.class, (settings, retentionPolicy, ticker) -> new ResetOnSnapshotConcurrentTop(settings));
        builders.put(ResetPeriodicallyRetentionPolicy.class, (settings, retentionPolicy, ticker) ->
                new ResetByChunksTop(settings, (ResetPeriodicallyRetentionPolicy) retentionPolicy, ticker));
        builders.put(ResetPeriodicallyByChunksRetentionPolicy.class, (settings, retentionPolicy, ticker) ->
                new ResetByChunksTop(settings, (ResetPeriodicallyByChunksRetentionPolicy) retentionPolicy, ticker));
    }

    private interface TopFactory {

        Top create(TopRecorderSettings settings, RetentionPolicy retentionPolicy, Ticker ticker);

    }


}
