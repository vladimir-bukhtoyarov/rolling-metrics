package com.github.addon.metrics.reporter;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MetricTimeUnits {
    private final TimeUnit defaultRate;
    private final TimeUnit defaultDuration;
    private final Map<String, TimeUnit> rateOverrides;
    private final Map<String, TimeUnit> durationOverrides;

    public MetricTimeUnits(TimeUnit defaultRate,
                           TimeUnit defaultDuration,
                           Map<String, TimeUnit> rateOverrides,
                           Map<String, TimeUnit> durationOverrides) {
        this.defaultRate = defaultRate;
        this.defaultDuration = defaultDuration;
        this.rateOverrides = rateOverrides;
        this.durationOverrides = durationOverrides;
    }

    public TimeUnit durationFor(String name) {
        return durationOverrides.containsKey(name) ? durationOverrides.get(name) : defaultDuration;
    }

    public TimeUnit rateFor(String name) {
        return rateOverrides.containsKey(name) ? rateOverrides.get(name) : defaultRate;
    }

}