package com.github.addon.metrics.decorator.timer;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.github.addon.metrics.decorator.MaximumThresholdExceedingCounter;
import com.github.addon.metrics.decorator.MinimumThresholdExceedingCounter;
import com.github.addon.metrics.decorator.UpdateListener;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class TimerDecoratorBuilder {

    private final List<UpdateListener> listeners = new ArrayList<>();
    private final Timer timer;

    public TimerDecoratorBuilder(Timer timer) {
        this.timer = Objects.requireNonNull(timer);
    }

    public Timer build() {
        return new TimerDecorator(timer, listeners);
    }

    public Timer buildAndRegister(String name, MetricRegistry registry) {
        Timer timer = build();
        registry.register(name, timer);
        return timer;
    }

    public TimerDecoratorBuilder withMinimumThreshold(Counter exceedingCounter, Duration thresholdDuration) {
        listeners.add(new MinimumThresholdExceedingCounter(exceedingCounter, thresholdDuration.toNanos()));
        return this;
    }

    public TimerDecoratorBuilder withMaximumThreshold(Counter exceedingCounter, Duration thresholdDuration) {
        listeners.add(new MaximumThresholdExceedingCounter(exceedingCounter, thresholdDuration.toNanos()));
        return this;
    }

}
