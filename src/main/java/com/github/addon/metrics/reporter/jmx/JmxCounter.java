package com.github.addon.metrics.reporter.jmx;

import com.codahale.metrics.Counter;
import com.github.addon.metrics.reporter.CachedValue;

import javax.management.ObjectName;
import java.time.Duration;

public class JmxCounter extends AbstractBean implements JmxCounterMBean {

    private final CachedValue<Long> count;

    JmxCounter(Counter metric, ObjectName objectName, Duration cachingDuration) {
        super(objectName);
        this.count = new CachedValue<>(cachingDuration, metric::getCount);
    }

    @Override
    public long getCount() {
        return count.get();
    }
}
