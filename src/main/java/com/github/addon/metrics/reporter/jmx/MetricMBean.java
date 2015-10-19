package com.github.addon.metrics.reporter.jmx;

import javax.management.ObjectName;

public interface MetricMBean {
    ObjectName objectName();
}
