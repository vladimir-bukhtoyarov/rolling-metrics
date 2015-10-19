package com.github.addon.metrics.reporter.jmx;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Snapshot;
import com.github.addon.metrics.reporter.CachedValue;

import javax.management.ObjectName;
import java.time.Duration;

class JmxHistogram extends AbstractBean implements JmxHistogramMBean {

    private final CachedValue<Long> count;
    private final CachedValue<Snapshot> snapshot;

    JmxHistogram(Histogram metric, ObjectName objectName, Duration cachingDuration) {
        super(objectName);
        this.count = new CachedValue<>(cachingDuration, metric::getCount);
        this.snapshot = new CachedValue<>(cachingDuration, metric::getSnapshot);
    }

    @Override
    public double get50thPercentile() {
        return snapshot.get().getMedian();
    }

    @Override
    public long getCount() {
        return count.get();
    }

    @Override
    public long getMin() {
        return snapshot.get().getMin();
    }

    @Override
    public long getMax() {
        return snapshot.get().getMax();
    }

    @Override
    public double getMean() {
        return snapshot.get().getMean();
    }

    @Override
    public double getStdDev() {
        return snapshot.get().getStdDev();
    }

    @Override
    public double get75thPercentile() {
        return snapshot.get().get75thPercentile();
    }

    @Override
    public double get95thPercentile() {
        return snapshot.get().get95thPercentile();
    }

    @Override
    public double get98thPercentile() {
        return snapshot.get().get98thPercentile();
    }

    @Override
    public double get99thPercentile() {
        return snapshot.get().get99thPercentile();
    }

    @Override
    public double get999thPercentile() {
        return snapshot.get().get999thPercentile();
    }

    @Override
    public long[] values() {
        return snapshot.get().getValues();
    }
}
