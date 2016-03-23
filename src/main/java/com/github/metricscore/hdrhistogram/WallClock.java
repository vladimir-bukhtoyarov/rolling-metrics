package com.github.metricscore.hdrhistogram;

public class WallClock {

    public static final WallClock INSTANCE = new WallClock();

    private WallClock() {}

    public static WallClock getInstance() {
        return INSTANCE;
    }

    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

}
