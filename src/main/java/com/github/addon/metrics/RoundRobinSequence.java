package com.github.addon.metrics;

import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinSequence {

    private final AtomicInteger sequence;
    private final int size;

    public RoundRobinSequence(int size) {
        this.size = size;
        sequence = new AtomicInteger(-1);
    }

    public int next() {
        while (true) {
            int currentSequenceValue = sequence.incrementAndGet();
            if (currentSequenceValue >= 0) {
                return currentSequenceValue % size;
            }

            // overflow detected, need to reset sequence to smallest integer equivalent
            int rescaledOffset = Integer.MAX_VALUE % size;
            while (currentSequenceValue < 0 && !sequence.compareAndSet(currentSequenceValue, rescaledOffset)) {
                currentSequenceValue = sequence.get();
            }
        }
    }

}
