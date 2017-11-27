package com.github.rollingmetrics.top.impl;

import com.github.rollingmetrics.retention.*;
import com.github.rollingmetrics.top.Top;
import com.github.rollingmetrics.top.TopBuilder;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DefaultTopBuilder implements TopBuilder {

    private final RetentionPolicy retentionPolicy;
    private final TopRecorderSettings settings;

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
        Top top = builders.get(retentionPolicy.getClass()).create(settings, retentionPolicy);
        if (!retentionPolicy.getSnapshotCachingDuration().isZero()) {
            top = new SnapshotCachingTop(top, retentionPolicy.getSnapshotCachingDuration(), retentionPolicy.getTicker());
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
    public TopBuilder withMaxLengthOfQueryDescription(int maxLengthOfQueryDescription) {
        settings.setMaxLengthOfQueryDescription(maxLengthOfQueryDescription);
        return this;
    }

    private static Map<Class<? extends RetentionPolicy>, TopFactory> builders = new HashMap<>();
    static {
        builders.put(UniformRetentionPolicy.class, (settings, retentionPolicy) -> new UniformTop(settings));
        builders.put(ResetOnSnapshotRetentionPolicy.class, (settings, retentionPolicy) -> new ResetOnSnapshotConcurrentTop(settings));
        builders.put(ResetPeriodicallyRetentionPolicy.class, (settings, retentionPolicy) ->
                new ResetByChunksTop(settings, (ResetPeriodicallyRetentionPolicy) retentionPolicy));
        builders.put(ResetPeriodicallyByChunksRetentionPolicy.class, (settings, retentionPolicy) ->
                new ResetByChunksTop(settings, (ResetPeriodicallyByChunksRetentionPolicy) retentionPolicy));
    }

    private interface TopFactory {

        Top create(TopRecorderSettings settings, RetentionPolicy retentionPolicy);

    }


}
