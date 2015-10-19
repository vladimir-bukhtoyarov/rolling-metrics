package com.github.addon.metrics.reporter.jmx;

import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import com.github.addon.metrics.CachedValue;

import javax.management.ObjectName;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class JmxTimer extends JmxMeter implements JmxTimerMBean {

    private final double durationFactor;
    private final String durationUnit;
    private final CachedValue<Snapshot> snapshot;

    JmxTimer(Timer metric, ObjectName objectName, TimeUnit rateUnit, TimeUnit durationUnit, Duration cachingDuration) {
        super(metric, objectName, rateUnit, cachingDuration);
        this.durationFactor = 1.0 / durationUnit.toNanos(1);
        this.durationUnit = durationUnit.toString().toLowerCase(Locale.US);
        this.snapshot = new CachedValue<>(cachingDuration, metric::getSnapshot);
    }

    @Override
    public double get50thPercentile() {
        return snapshot.get().getMedian() * durationFactor;
    }

    @Override
    public double getMin() {
        return snapshot.get().getMin() * durationFactor;
    }

    @Override
    public double getMax() {
        return snapshot.get().getMax() * durationFactor;
    }

    @Override
    public double getMean() {
        return snapshot.get().getMean() * durationFactor;
    }

    @Override
    public double getStdDev() {
        return snapshot.get().getStdDev() * durationFactor;
    }

    @Override
    public double get75thPercentile() {
        return snapshot.get().get75thPercentile() * durationFactor;
    }

    @Override
    public double get95thPercentile() {
        return snapshot.get().get95thPercentile() * durationFactor;
    }

    @Override
    public double get98thPercentile() {
        return snapshot.get().get98thPercentile() * durationFactor;
    }

    @Override
    public double get99thPercentile() {
        return snapshot.get().get99thPercentile() * durationFactor;
    }

    @Override
    public double get999thPercentile() {
        return snapshot.get().get999thPercentile() * durationFactor;
    }

    @Override
    public long[] values() {
        return snapshot.get().getValues();
    }

    @Override
    public String getDurationUnit() {
        return durationUnit;
    }
}
