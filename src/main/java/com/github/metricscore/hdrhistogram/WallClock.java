package com.github.metricscore.hdrhistogram;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Wrapper around time measuring useful in unit tests to avoid sleeping.
 */
class WallClock {

    public static final WallClock INSTANCE = new WallClock();

    private WallClock() {}

    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    public static WallClock mock(AtomicLong currentTimeProvider) {
        return new WallClock() {
            @Override
            public long currentTimeMillis() {
                return currentTimeProvider.get();
            }
        };
    }

}
