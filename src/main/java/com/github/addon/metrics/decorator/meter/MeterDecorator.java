package com.github.addon.metrics.decorator.meter;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import com.github.addon.metrics.decorator.UpdateListener;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class MeterDecorator extends Meter {

    private final Meter meter;
    private final List<UpdateListener> listeners;

    public MeterDecorator(Meter meter, List<UpdateListener> listeners) {
        this.meter = meter;
        this.listeners = listeners;
    }

    @Override
    public void mark() {
        mark(1);
    }

    @Override
    public void mark(long n) {
        meter.mark(n);
        for (UpdateListener listener : listeners) {
            listener.onUpdate(n);
        }
    }

    @Override
    public long getCount() {
        return meter.getCount();
    }

    @Override
    public double getFifteenMinuteRate() {
        return meter.getFifteenMinuteRate();
    }

    @Override
    public double getFiveMinuteRate() {
        return meter.getFiveMinuteRate();
    }

    @Override
    public double getMeanRate() {
        return meter.getMeanRate();
    }

    @Override
    public double getOneMinuteRate() {
        return meter.getOneMinuteRate();
    }

}
