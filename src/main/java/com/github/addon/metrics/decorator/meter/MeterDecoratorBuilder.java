package com.github.addon.metrics.decorator.meter;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.github.addon.metrics.decorator.MaximumThresholdExceedingCounter;
import com.github.addon.metrics.decorator.MinimumThresholdExceedingCounter;
import com.github.addon.metrics.decorator.UpdateListener;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class MeterDecoratorBuilder {

    private final List<UpdateListener> listeners = new ArrayList<>();
    private final Meter meter;

    public MeterDecoratorBuilder(Meter meter) {
        this.meter = Objects.requireNonNull(meter);
    }

    public Meter build() {
        return new MeterDecorator(meter, listeners);
    }

    public Meter buildAndRegister(String name, MetricRegistry registry) {
        Meter meter = build();
        registry.register(name, meter);
        return meter;
    }

    public MeterDecoratorBuilder withMinimumThreshold(Counter exceedingCounter, long threshold) {
        listeners.add(new MinimumThresholdExceedingCounter(exceedingCounter, threshold));
        return this;
    }

    public MeterDecoratorBuilder withMaximumThreshold(Counter exceedingCounter, long threshold) {
        listeners.add(new MaximumThresholdExceedingCounter(exceedingCounter, threshold));
        return this;
    }

}
