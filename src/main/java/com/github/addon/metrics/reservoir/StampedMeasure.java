package com.github.addon.metrics.reservoir;

public class StampedMeasure {

    private final long unixTimestamp;
    private final long value;

    public StampedMeasure(long unixTimestamp, long value) {
        this.unixTimestamp = unixTimestamp;
        this.value = value;
    }

    public long getUnixTimestamp() {
        return unixTimestamp;
    }

    public long getValue() {
        return value;
    }

    public long getAgeInMillis(long currentTimeMillis) {
        return currentTimeMillis - unixTimestamp;
    }

}
