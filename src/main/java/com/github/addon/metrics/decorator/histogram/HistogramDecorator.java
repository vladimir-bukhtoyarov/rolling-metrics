package com.github.addon.metrics.decorator.histogram;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Snapshot;
import com.github.addon.metrics.decorator.UpdateListener;

import java.util.List;

public class HistogramDecorator extends Histogram {

    private final Histogram histogram;
    private final List<UpdateListener> listeners;

    public HistogramDecorator(Histogram histogram, List<UpdateListener> listeners) {
        super(null);
        this.histogram = histogram;
        this.listeners = listeners;
    }

    @Override
    public void update(int value) {
        update((long) value);
    }

    @Override
    public void update(long value) {
        histogram.update(value);
        for (UpdateListener listener : listeners) {
            listener.onUpdate(value);
        }
    }

    @Override
    public long getCount() {
        return histogram.getCount();
    }

    @Override
    public Snapshot getSnapshot() {
        return histogram.getSnapshot();
    }

}
