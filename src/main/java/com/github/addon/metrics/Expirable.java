package com.github.addon.metrics;

public interface Expirable {

    long getExpirationTimestamp();

    boolean isExpired(long currentTimestamp);

}
