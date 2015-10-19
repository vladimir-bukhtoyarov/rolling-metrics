package com.github.addon.metrics;

import com.codahale.metrics.Clock;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Supplier;

public class CachedValue<T> {

    private final StampedLock stampedLock;
    private final long maxAgeMillis;
    private final Clock clock;
    private final Supplier<T> supplier;

    private T value;
    private long expirationTimestamp;

    public CachedValue(Duration cachingDuration, Supplier<T> supplier) {
        this(cachingDuration, supplier, Clock.defaultClock());
    }

    public CachedValue(Duration cachingDuration, Supplier<T> supplier, Clock clock) {
        this.maxAgeMillis = cachingDuration.toMillis();
        this.supplier = Objects.requireNonNull(supplier);
        this.clock = Objects.requireNonNull(clock);
        this.stampedLock = new StampedLock();
    }

    public T get() {
        long currentTimeMillis = clock.getTime();

        // try optimistic read
        long stamp = stampedLock.tryOptimisticRead();
        if (stamp != 0) {
            T valueLocal = this.value;
            long expirationTimestampLocal = this.expirationTimestamp;
            if (valueLocal != null && currentTimeMillis <= expirationTimestampLocal && stampedLock.validate(stamp)) {
                return valueLocal;
            }
        }

        // conditionally update
        stamp = stampedLock.readLock();
        try {
            while (this.value == null || currentTimeMillis > this.expirationTimestamp) {
                long writeStamp = stampedLock.tryConvertToWriteLock(stamp);
                if (writeStamp != 0) {
                    stamp = writeStamp;
                    this.value = supplier.get();
                    this.expirationTimestamp = currentTimeMillis + maxAgeMillis;
                    return this.value;
                } else {
                    stampedLock.unlockRead(stamp);
                    stamp = stampedLock.writeLock();
                }
            }
            return value;
        } finally {
            stampedLock.unlock(stamp);
        }
    }

}
