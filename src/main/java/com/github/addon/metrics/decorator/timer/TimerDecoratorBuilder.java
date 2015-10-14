package com.github.addon.metrics.decorator.timer;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class TimerDecoratorBuilder {

    private final List<TimerListener> listeners = new ArrayList<>();
    private final Timer timer;
    private SnapshotExtractor snapshotExtractor = SnapshotExtractor.DEFAULT;

    public TimerDecoratorBuilder(Timer timer) {
        this.timer = Objects.requireNonNull(timer);
    }

    public Timer build() {
        return new TimerDecorator(timer, listeners, snapshotExtractor);
    }

    public TimerDecoratorBuilder withMinimumExceedingThresholdCounter(Counter exceedingCounter, Duration thresholdDuration) {
        listeners.add(new MinimumThresholdExceedingCounter(exceedingCounter, thresholdDuration));
        return this;
    }

    public TimerDecoratorBuilder withMaximumExceedingThresholdCounter(Counter exceedingCounter, Duration thresholdDuration) {
        listeners.add(new MaximumThresholdExceedingCounter(exceedingCounter, thresholdDuration));
        return this;
    }

    public TimerDecoratorBuilder withSnapshotCachingDuration(Duration cachingDuration) {
        this.snapshotExtractor = new SnapshotCachingExtractor(cachingDuration);
        return this;
    }

}
