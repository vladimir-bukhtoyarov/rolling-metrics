package com.github.addon.metrics;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Sampling;
import com.codahale.metrics.Snapshot;

import java.time.Duration;
import java.util.concurrent.locks.StampedLock;

public class SnapshotCachingExtractor implements SnapshotExtractor {

    private final StampedLock stampedLock;
    private final long maxAgeMillis;
    private final Clock clock;

    private Snapshot snapshot;
    private long expirationTimestamp;

    public SnapshotCachingExtractor(Duration cachingDuration) {
        this(cachingDuration, Clock.defaultClock());
    }

    SnapshotCachingExtractor(Duration cachingDuration, Clock clock) {
        this.maxAgeMillis = cachingDuration.toMillis();
        this.clock = clock;
        this.stampedLock = new StampedLock();
    }

    @Override
    public Snapshot extract(Sampling sampling) {
        long currentTimeMillis = clock.getTime();

        // try optimistic read
        long stamp = stampedLock.tryOptimisticRead();
        if (stamp != 0) {
            Snapshot snapshotLocal = this.snapshot;
            long expirationTimestampLocal = this.expirationTimestamp;
            if (snapshotLocal != null && currentTimeMillis <= expirationTimestampLocal && stampedLock.validate(stamp)) {
                return snapshotLocal;
            }
        }

        stamp = stampedLock.readLock();
        try {
            while (this.snapshot == null || currentTimeMillis > this.expirationTimestamp) {
                long writeStamp = stampedLock.tryConvertToWriteLock(stamp);
                if (writeStamp != 0) {
                    stamp = writeStamp;
                    snapshot = sampling.getSnapshot();
                    expirationTimestamp = currentTimeMillis + maxAgeMillis;
                    return snapshot;
                } else {
                    stampedLock.unlockRead(stamp);
                    stamp = stampedLock.writeLock();
                }
            }
            return snapshot;
        } finally {
            stampedLock.unlock(stamp);
        }
    }

}
