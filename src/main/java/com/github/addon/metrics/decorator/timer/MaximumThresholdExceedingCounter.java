package com.github.addon.metrics.decorator.timer;

import com.codahale.metrics.Counter;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class MaximumThresholdExceedingCounter implements TimerListener {

    private final Counter exceedingCounter;
    private final long maximumDurationNanos;

    public MaximumThresholdExceedingCounter(Counter exceedingCounter, Duration maximumDuration) {
        this.exceedingCounter = Objects.requireNonNull(exceedingCounter);
        this.maximumDurationNanos = maximumDuration.toNanos();
    }

    @Override
    public void onUpdate(long duration, TimeUnit unit) {
        if (unit.toNanos(duration) > maximumDurationNanos) {
            exceedingCounter.inc();
        }
    }

}
