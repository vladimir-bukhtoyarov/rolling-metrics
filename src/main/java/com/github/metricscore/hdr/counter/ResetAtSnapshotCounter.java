package com.github.metricscore.hdr.counter;

import org.HdrHistogram.WriterReaderPhaser;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Created by vladimir.bukhtoyarov on 02.09.2016.
 */
public class ResetAtSnapshotCounter implements TimeWindowCounter {

    private final LongAdder adder = new LongAdder();

    private volatile LongAdder activeAdder;
    private LongAdder inactiveAdder;

    public ResetAtSnapshotCounter() {
        super();
    }

    @Override
    public void increment(long value, long measureTimestampMillis) {
        if (value < 1) {
            throw new IllegalArgumentException("value should be >= 1");
        }
        adder.add(value);
    }

    @Override
    public Long getValue() {
        long sum = adder.sum();
        adder.add(-sum);
        return sum;
    }

}
