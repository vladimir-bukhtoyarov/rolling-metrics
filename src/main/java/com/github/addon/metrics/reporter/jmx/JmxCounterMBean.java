package com.github.addon.metrics.reporter.jmx;

public interface JmxCounterMBean extends MetricMBean {
    long getCount();
}
