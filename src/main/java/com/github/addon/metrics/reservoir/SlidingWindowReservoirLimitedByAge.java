package com.github.addon.metrics.reservoir;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.UniformSnapshot;
import com.github.addon.metrics.RingSequence;

import java.time.Duration;
import java.util.Arrays;

public class SlidingWindowReservoirLimitedByAge implements Reservoir {

    private final ExpirableMeasure[] measurements;
    private final RingSequence sequence;
    private final long maxAgeMillis;
    private final Clock clock;

    public SlidingWindowReservoirLimitedByAge(int size, Duration maxAge) {
        this(size, maxAge, Clock.defaultClock());
    }

    public SlidingWindowReservoirLimitedByAge(int size, Duration maxAge, Clock clock) {
        if (maxAge.isNegative() || maxAge.isZero()) {
            throw new IllegalArgumentException("maxAge should be positive");
        }
        this.measurements = new ExpirableMeasure[size];
        this.sequence = new RingSequence(size);
        this.maxAgeMillis = maxAge.toMillis();
        this.clock = clock;
    }

    // unnecessary method https://github.com/dropwizard/metrics/issues/874
    @Override
    public int size() {
        // I hope "size" is called infrequently
        return getSnapshot().size();
    }

    @Override
    public void update(long value) {
        long expirationTimeStamp = clock.getTime() + maxAgeMillis;
        int index = sequence.next();
        measurements[index] = new ExpirableMeasure(value, expirationTimeStamp);
    }

    @Override
    public Snapshot getSnapshot() {
        long currentTimeMillis = clock.getTime();
        ExpirableMeasure[] measurementSnapshot = Arrays.copyOf(measurements, measurements.length);
        long[] values = new long[measurementSnapshot.length];
        int valuesCount = 0;
        for (ExpirableMeasure measure : measurementSnapshot) {
            if (!measure.isExpired(currentTimeMillis)) {
                values[valuesCount] = measure.getValue();
                valuesCount++;
            }
        }
        if (valuesCount < values.length) {
            values = Arrays.copyOf(values, valuesCount);
        }

        return new UniformSnapshot(values);
    }

}
