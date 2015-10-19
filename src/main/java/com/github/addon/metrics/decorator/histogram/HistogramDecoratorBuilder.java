package com.github.addon.metrics.decorator.histogram;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.github.addon.metrics.decorator.MaximumThresholdExceedingCounter;
import com.github.addon.metrics.decorator.MinimumThresholdExceedingCounter;
import com.github.addon.metrics.decorator.UpdateListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class HistogramDecoratorBuilder {

    private final List<UpdateListener> listeners = new ArrayList<>();
    private final Histogram histogram;

    public HistogramDecoratorBuilder(Histogram histogram) {
        this.histogram = Objects.requireNonNull(histogram);
    }

    public Histogram build() {
        return new HistogramDecorator(histogram, listeners);
    }

    public Histogram buildAndRegister(String name, MetricRegistry registry) {
        Histogram histogram = build();
        registry.register(name, histogram);
        return histogram;
    }

    public HistogramDecoratorBuilder withMinimumThreshold(Counter exceedingCounter, long threshold) {
        listeners.add(new MinimumThresholdExceedingCounter(exceedingCounter, threshold));
        return this;
    }

    public HistogramDecoratorBuilder withMaximumThreshold(Counter exceedingCounter, long threshold) {
        listeners.add(new MaximumThresholdExceedingCounter(exceedingCounter, threshold));
        return this;
    }

}
