package com.github.addon.metrics.reporter.jmx;

public interface JmxGaugeMBean extends MetricMBean {
    Object getValue();
}
