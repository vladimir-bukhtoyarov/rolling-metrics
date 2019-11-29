package com.github.rollingmetrics.micrometer.meters;

import com.github.rollingmetrics.util.Ticker;
import io.micrometer.core.instrument.Clock;

public class TickerClock implements Ticker {

    private final long initializationNanoTime;
    private Clock clock;

    public TickerClock(Clock clock) {
        this.clock = clock;
        initializationNanoTime = clock.monotonicTime();
    }

    @Override
    public long nanoTime() {
        return clock.monotonicTime();
    }

    @Override
    public long stableMilliseconds() {
        return  (nanoTime() - initializationNanoTime) / 1_000_000;
    }
}
