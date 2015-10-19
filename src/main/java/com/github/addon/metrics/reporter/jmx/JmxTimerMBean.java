package com.github.addon.metrics.reporter.jmx;

public interface JmxTimerMBean extends JmxMeterMBean {
    double getMin();

    double getMax();

    double getMean();

    double getStdDev();

    double get50thPercentile();

    double get75thPercentile();

    double get95thPercentile();

    double get98thPercentile();

    double get99thPercentile();

    double get999thPercentile();

    long[] values();

    String getDurationUnit();
}
