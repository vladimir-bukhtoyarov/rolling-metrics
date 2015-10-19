package com.github.addon.metrics.reservoir;

import com.github.addon.metrics.reservoir.Expirable;

public class ExpirableMeasure implements Expirable {

    private final long expirationTimestamp;
    private final long value;

    public ExpirableMeasure(long value, long expirationTimestamp) {
        this.expirationTimestamp = expirationTimestamp;
        this.value = value;
    }

    public long getValue() {
        return value;
    }

    @Override
    public long getExpirationTimestamp() {
        return expirationTimestamp;
    }

    @Override
    public boolean isExpired(long currentTimestamp) {
        return currentTimestamp > expirationTimestamp;
    }

}
