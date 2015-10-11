package com.github.addon.metrics.decorator.timer;

import com.codahale.metrics.Counter;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class MinimumThresholdExceedingCounter implements TimerListener {

    private final Counter exceedingCounter;
    private final long minimalDurationNanos;

    public MinimumThresholdExceedingCounter(Counter exceedingCounter, Duration minimalDuration) {
        this.exceedingCounter = Objects.requireNonNull(exceedingCounter);
        this.minimalDurationNanos = minimalDuration.toNanos();
    }

    @Override
    public void onUpdate(long duration, TimeUnit unit) {
        if (unit.toNanos(duration) < minimalDurationNanos) {
            exceedingCounter.inc();
        }
    }

}
