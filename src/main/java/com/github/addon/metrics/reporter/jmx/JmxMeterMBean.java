package com.github.addon.metrics.reporter.jmx;

public interface JmxMeterMBean extends MetricMBean {
    long getCount();

    double getMeanRate();

    double getOneMinuteRate();

    double getFiveMinuteRate();

    double getFifteenMinuteRate();

    String getRateUnit();
}
