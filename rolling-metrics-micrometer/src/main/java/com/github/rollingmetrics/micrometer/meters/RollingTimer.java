package com.github.rollingmetrics.micrometer.meters;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.distribution.HistogramSnapshot;
import io.micrometer.core.instrument.util.TimeUtils;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class RollingTimer implements Timer {
    private final RollingDistributionSummary distributionSummary;
    private final Clock clock;

    public RollingTimer(RollingDistributionSummary distributionSummary, Clock clock) {
        this.distributionSummary = distributionSummary;
        this.clock = clock;
    }

    @Override
    public void record(long amount, TimeUnit unit) {
        distributionSummary.record(TimeUtils.convert(amount, unit, TimeUnit.MILLISECONDS));
    }

    @Override
    public <T> T record(Supplier<T> f) {
        long startNanos = clock.monotonicTime();
        try {
            return f.get();
        } finally {
            record(clock.monotonicTime() - startNanos, TimeUnit.NANOSECONDS);
        }
    }

    @Override
    public <T> T recordCallable(Callable<T> f) throws Exception {
        long startNanos = clock.monotonicTime();
        try {
            return f.call();
        } finally {
            record(clock.monotonicTime() - startNanos, TimeUnit.NANOSECONDS);
        }
    }

    @Override
    public void record(Runnable f) {
        long startNanos = clock.monotonicTime();
        try {
            f.run();
        } finally {
            record(clock.monotonicTime() - startNanos, TimeUnit.NANOSECONDS);
        }
    }

    @Override
    public long count() {
        return distributionSummary.count();
    }

    @Override
    public double totalTime(TimeUnit unit) {
        return TimeUtils.convert(distributionSummary.totalAmount(), TimeUnit.MILLISECONDS, unit);
    }

    @Override
    public double max(TimeUnit unit) {
        return TimeUtils.convert(distributionSummary.max(), TimeUnit.MILLISECONDS, unit);
    }

    @Override
    public TimeUnit baseTimeUnit() {
        return TimeUnit.MILLISECONDS;
    }

    @Override
    public HistogramSnapshot takeSnapshot() {
        return distributionSummary.takeSnapshot();
    }

    @Override
    public Id getId() {
        return distributionSummary.getId();
    }
}
