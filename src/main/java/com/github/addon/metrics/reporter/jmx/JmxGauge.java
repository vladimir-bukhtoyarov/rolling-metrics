package com.github.addon.metrics.reporter.jmx;

import com.codahale.metrics.Gauge;
import com.github.addon.metrics.CachedValue;

import javax.management.ObjectName;
import java.time.Duration;

public class JmxGauge extends AbstractBean implements JmxGaugeMBean {

    private final CachedValue<?> value;

    JmxGauge(Gauge<?> metric, ObjectName objectName, Duration cachingDuration) {
        super(objectName);
        this.value = new CachedValue<>(cachingDuration, metric::getValue);
    }

    @Override
    public Object getValue() {
        return value.get();
    }

}
