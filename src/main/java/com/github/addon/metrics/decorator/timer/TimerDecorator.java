package com.github.addon.metrics.decorator.timer;

import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class TimerDecorator extends Timer {

    private final Timer timer;
    private final List<TimerListener> listeners;

    public TimerDecorator(Timer timer, List<TimerListener> listeners) {
        this.timer = timer;
        this.listeners = listeners;
    }

    @Override
    public void update(long duration, TimeUnit unit) {
        timer.update(duration, unit);
        for (TimerListener listener : listeners) {
            listener.onUpdate(duration, unit);
        }
    }

    public <T> T time(Callable<T> event) throws Exception {
        return timer.time(event);
    }

    public Context time() {
        return timer.time();
    }

    @Override
    public long getCount() {
        return timer.getCount();
    }

    @Override
    public double getFifteenMinuteRate() {
        return timer.getFifteenMinuteRate();
    }

    @Override
    public double getFiveMinuteRate() {
        return timer.getFiveMinuteRate();
    }

    @Override
    public double getMeanRate() {
        return timer.getMeanRate();
    }

    @Override
    public double getOneMinuteRate() {
        return timer.getOneMinuteRate();
    }

    @Override
    public Snapshot getSnapshot() {
        return timer.getSnapshot();
    }

}
