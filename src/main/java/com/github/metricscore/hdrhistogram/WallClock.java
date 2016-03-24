package com.github.metricscore.hdrhistogram;

/**
 * Wrapper around time measuring useful in unit tests to avoid sleeping.
 */
class WallClock {

    public static final WallClock INSTANCE = new WallClock();

    private WallClock() {}

    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

}
