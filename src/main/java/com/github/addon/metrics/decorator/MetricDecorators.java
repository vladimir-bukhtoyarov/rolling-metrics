package com.github.addon.metrics.decorator;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.github.addon.metrics.decorator.histogram.HistogramDecoratorBuilder;
import com.github.addon.metrics.decorator.meter.MeterDecoratorBuilder;
import com.github.addon.metrics.decorator.timer.TimerDecoratorBuilder;

public class MetricDecorators {

    public static TimerDecoratorBuilder forTimer(Timer timer) {
        return new TimerDecoratorBuilder(timer);
    }

    public static TimerDecoratorBuilder forTimer() {
        return forTimer(new Timer());
    }

    public static MeterDecoratorBuilder forMeter(Meter meter) {
        return new MeterDecoratorBuilder(meter);
    }

    public static MeterDecoratorBuilder forMeter() {
        return forMeter(new Meter());
    }

    public static HistogramDecoratorBuilder forHistogram(Histogram histogram) {
        return new HistogramDecoratorBuilder(histogram);
    }

}
