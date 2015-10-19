package com.github.addon.metrics.reporter.jmx;

import javax.management.ObjectName;

public class AbstractBean implements MetricMBean {

    private final ObjectName objectName;

    protected AbstractBean(ObjectName objectName) {
        this.objectName = objectName;
    }

    @Override
    public ObjectName objectName() {
        return objectName;
    }

}