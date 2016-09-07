/*
 *
 *  Copyright 2016 Vladimir Bukhtoyarov
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.github.metricscore.hdr.counter;

import com.codahale.metrics.Clock;

import java.util.concurrent.atomic.AtomicLong;

class ResetPeriodicallyCounter implements WindowCounter {

    private final AtomicLong value = new AtomicLong();
    private final long resetIntervalMillis;
    private final Clock clock;
    private final AtomicLong nextResetTimeMillisRef;

    ResetPeriodicallyCounter(long resetIntervalMillis, Clock clock) {
        this.resetIntervalMillis = resetIntervalMillis;
        this.clock = clock;
        this.nextResetTimeMillisRef = new AtomicLong(clock.getTime() + resetIntervalMillis);
    }

    @Override
    public void add(long delta) {
        if (delta < 1) {
            throw new IllegalArgumentException("value should be >= 1");
        }
        while (true) {
            long nextResetTimeMillis = nextResetTimeMillisRef.get();
            long currentTimeMillis = clock.getTime();
            if (currentTimeMillis < nextResetTimeMillis) {
                value.addAndGet(delta);
                return;
            }
            long currentValue = value.get();
            if (nextResetTimeMillisRef.compareAndSet(nextResetTimeMillis, Long.MAX_VALUE)) {
                value.addAndGet(delta - currentValue);
                nextResetTimeMillisRef.set(currentTimeMillis + resetIntervalMillis);
                return;
            }
        }
    }

    @Override
    synchronized public long getSum() {
        while (true) {
            long nextResetTimeMillis = nextResetTimeMillisRef.get();
            long currentTimeMillis = clock.getTime();
            if (currentTimeMillis < nextResetTimeMillis) {
                return value.get();
            }
            long currentValue = value.get();
            if (nextResetTimeMillisRef.compareAndSet(nextResetTimeMillis, Long.MAX_VALUE)) {
                long result = value.addAndGet(-currentValue);
                nextResetTimeMillisRef.set(currentTimeMillis + resetIntervalMillis);
                return result;
            }
        }
    }

    @Override
    synchronized public Long getValue() {
        return getSum();
    }

    @Override
    public String toString() {
        return "ResetPeriodicallyCounter{" +
                "value=" + value +
                ", resetIntervalMillis=" + resetIntervalMillis +
                ", clock=" + clock +
                ", nextResetTimeMillisRef=" + nextResetTimeMillisRef +
                '}';
    }

}
