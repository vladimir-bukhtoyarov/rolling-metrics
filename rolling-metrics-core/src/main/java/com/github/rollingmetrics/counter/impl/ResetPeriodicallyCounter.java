/*
 *    Copyright 2017 Vladimir Bukhtoyarov
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package com.github.rollingmetrics.counter.impl;

import com.github.rollingmetrics.counter.WindowCounter;
import com.github.rollingmetrics.retention.ResetPeriodicallyRetentionPolicy;
import com.github.rollingmetrics.util.Ticker;

import java.util.concurrent.atomic.AtomicLong;

/**
 * The counter which reset its state to zero each time when configured interval is elapsed.
 *
 * <p>
 * Concurrency properties:
 * <ul>
 *     <li>Writing is lock-free.</li>
 *     <li>Sum reading is lock-free.</li>
 * </ul>
 *
 * <p>
 * Usage recommendations:
 * <ul>
 *     <li>When you do not need in "rolling time window" semantic. Else use {@link SmoothlyDecayingRollingCounter}</li>
 *     <li>When you want to limit time which each increment takes affect to counter sum in order to avoid reporting of obsolete measurements.</li>
 *     <li>Only if you accept the fact that several increments can be never observed by reader(because rotation to zero can happen before reader seen the written values).</li>
 * </ul>
 *
 * @see SmoothlyDecayingRollingCounter
 */
class ResetPeriodicallyCounter implements WindowCounter {

    private final AtomicLong value = new AtomicLong();
    private final long resetIntervalMillis;
    private final Ticker ticker;
    private final AtomicLong nextResetTimeMillisRef;

    /**
     * TODO
     * Constructs the counter which reset its state to zero each time when {@code resetInterval} is elapsed.
     *
     * @param retentionPolicy
     * @param ticker
     */
    ResetPeriodicallyCounter(ResetPeriodicallyRetentionPolicy retentionPolicy, Ticker ticker) {
        this.resetIntervalMillis = retentionPolicy.getResettingPeriodMillis();
        this.ticker = ticker;
        this.nextResetTimeMillisRef = new AtomicLong(ticker.stableMilliseconds() + resetIntervalMillis);
    }

    @Override
    public void add(long delta) {
        while (true) {
            long nextResetTimeMillis = nextResetTimeMillisRef.get();
            long currentTimeMillis = ticker.stableMilliseconds();
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
    public long getSum() {
        while (true) {
            long nextResetTimeMillis = nextResetTimeMillisRef.get();
            long currentValue = value.get();
            long currentTimeMillis = ticker.stableMilliseconds();
            if (currentTimeMillis < nextResetTimeMillis) {
                return currentValue;
            }

            if (nextResetTimeMillisRef.compareAndSet(nextResetTimeMillis, Long.MAX_VALUE)) {
                value.addAndGet(-currentValue);
                nextResetTimeMillisRef.set(currentTimeMillis + resetIntervalMillis);
                return value.get();
            }
        }
    }

    @Override
    public String toString() {
        return "ResetPeriodicallyCounter{" +
                "value=" + value +
                ", resetIntervalMillis=" + resetIntervalMillis +
                ", ticker=" + ticker +
                ", nextResetTimeMillisRef=" + nextResetTimeMillisRef +
                '}';
    }

}
