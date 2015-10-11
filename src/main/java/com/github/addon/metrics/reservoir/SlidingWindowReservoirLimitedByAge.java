package com.github.addon.metrics.reservoir;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.UniformSnapshot;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A {@link com.codahale.metrics.Reservoir} implementation backed by a sliding window that stores the last {@code N}
 * measurements, but restricts maximum age of measurements in the snapshot.
 *
 * This implementation is non-blocking and a little bit roughly at snapshot
 */
public class SlidingWindowReservoirLimitedByAge implements Reservoir {

    private final ReentrantLock rescalingLock = new ReentrantLock();

    private final StampedMeasure[] measurements;
    private final AtomicInteger sequence;
    private final long maxAgeMillis;
    private final Clock clock;

    /**
     * Creates a new {@link SlidingWindowReservoirLimitedByAge} which stores the last {@code size} measurements.
     * The measurements which age older then {@code maxAge} will be always excluded from snapshot.
     *
     * @param size the number of measurements to store
     */
    public SlidingWindowReservoirLimitedByAge(int size, Duration maxAge) {
        this(size, maxAge, Clock.defaultClock());
    }

    public SlidingWindowReservoirLimitedByAge(int size, Duration maxAge, Clock clock) {
        this.measurements = new StampedMeasure[size];
        this.sequence = new AtomicInteger(0);
        this.maxAgeMillis = maxAge.toMillis();
        this.clock = clock;
    }

    @Override
    public int size() {
        return Math.min(measurements.length, this.sequence.get());
    }

    @Override
    public void update(long value) {
        long timeMillis = clock.getTime();
        int index = getRescaledOffset();
        StampedMeasure stampedMeasure = new StampedMeasure(timeMillis, value);
        measurements[index] = stampedMeasure;
    }

    private int getRescaledOffset() {
        while (true) {
            int currentSequenceValue = this.sequence.incrementAndGet();
            if (currentSequenceValue > 0 && currentSequenceValue < Integer.MAX_VALUE) {
                return currentSequenceValue % measurements.length;
            }
            if (currentSequenceValue < 0) {
                // overflow detected, but current thread is not the thread which detected overflow firstly.
                // Do spin wait until thread which detects overflow performs rescaling
                while (sequence.get() < 0) {
                    Thread.yield();
                }
                continue;
            }
            if (currentSequenceValue == Integer.MAX_VALUE) {
                // overflow detected and current thread is responsible for rescaling
                int offset = Integer.MAX_VALUE % measurements.length;
                this.sequence.set(measurements.length + offset);
                return offset;
            }
        }
    }

    @Override
    public Snapshot getSnapshot() {
        long currentTimeMillis = clock.getTime();
        StampedMeasure[] measurementSnapshot = Arrays.copyOf(measurements, size());
        long[] values = new long[measurementSnapshot.length];
        int valuesCount = 0;
        for (StampedMeasure measure : measurementSnapshot) {
            if (measure.getAgeInMillis(currentTimeMillis) <= maxAgeMillis) {
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
