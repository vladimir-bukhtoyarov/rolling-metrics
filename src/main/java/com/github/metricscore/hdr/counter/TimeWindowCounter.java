package com.github.metricscore.hdr.counter;

import com.codahale.metrics.Gauge;

public interface TimeWindowCounter extends Gauge<Long> {

    default void increment() {
        increment(1, System.currentTimeMillis());
    }

    default void increment(long value) {
        increment(value, System.currentTimeMillis());
    }

    void increment(long value, long measureTimestampMillis);

}
