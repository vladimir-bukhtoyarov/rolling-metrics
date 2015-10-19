package com.github.addon.metrics.reservoir;

public interface Expirable {

    long getExpirationTimestamp();

    boolean isExpired(long currentTimestamp);

}
