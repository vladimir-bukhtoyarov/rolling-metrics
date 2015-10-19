package com.github.addon.metrics.reporter.jmx;

import com.codahale.metrics.Metered;
import com.github.addon.metrics.reporter.CachedValue;

import javax.management.ObjectName;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class JmxMeter extends AbstractBean implements JmxMeterMBean {

    private final double rateFactor;
    private final String rateUnit;
    private final CachedValue<Long> count;
    private final CachedValue<Double> meanRate;
    private final CachedValue<Double> oneMinuteRate;
    private final CachedValue<Double> fiveMinuteRate;
    private final CachedValue<Double> fifteenMinuteRate;

    JmxMeter(Metered metric, ObjectName objectName, TimeUnit rateUnit, Duration cachingDuration) {
        super(objectName);
        this.rateFactor = rateUnit.toSeconds(1);
        this.rateUnit = "events/" + calculateRateUnit(rateUnit);

        this.count = new CachedValue<>(cachingDuration, metric::getCount);
        this.meanRate = new CachedValue<>(cachingDuration, metric::getMeanRate);
        this.oneMinuteRate = new CachedValue<>(cachingDuration, metric::getOneMinuteRate);
        this.fiveMinuteRate = new CachedValue<>(cachingDuration, metric::getFiveMinuteRate);
        this.fifteenMinuteRate = new CachedValue<>(cachingDuration, metric::getFifteenMinuteRate);
    }

    @Override
    public long getCount() {
        return count.get();
    }

    @Override
    public double getMeanRate() {
        return meanRate.get() * rateFactor;
    }

    @Override
    public double getOneMinuteRate() {
        return oneMinuteRate.get() * rateFactor;
    }

    @Override
    public double getFiveMinuteRate() {
        return fiveMinuteRate.get() * rateFactor;
    }

    @Override
    public double getFifteenMinuteRate() {
        return fifteenMinuteRate.get() * rateFactor;
    }

    @Override
    public String getRateUnit() {
        return rateUnit;
    }

    private String calculateRateUnit(TimeUnit unit) {
        final String s = unit.toString().toLowerCase(Locale.US);
        return s.substring(0, s.length() - 1);
    }
}
