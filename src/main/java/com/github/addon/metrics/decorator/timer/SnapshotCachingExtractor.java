package com.github.addon.metrics.decorator.timer;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class SnapshotCachingExtractor implements SnapshotExtractor {

    private final AtomicLong lastSnapshotTime = new AtomicLong(0);
    private final AtomicReference<Snapshot> snapshotReference = new AtomicReference<>();
    private final long cachingDurationMillis;
    private final Clock clock;

    public SnapshotCachingExtractor(Duration cachingDuration) {
        this(cachingDuration, Clock.defaultClock());
    }

    SnapshotCachingExtractor(Duration cachingDuration, Clock clock) {
        this.cachingDurationMillis = cachingDuration.toMillis();
        this.clock = clock;
    }

    @Override
    public Snapshot extract(Timer timer) {
        long now = clock.getTime();
        Snapshot cachedSnapshot = snapshotReference.get();
        if (cachedSnapshot == null || now >= lastSnapshotTime.get() + cachingDurationMillis) {
            cachedSnapshot = timer.getSnapshot();
            lastSnapshotTime.set(now);
            snapshotReference.set(cachedSnapshot);
        }
        return cachedSnapshot;
    }

}
