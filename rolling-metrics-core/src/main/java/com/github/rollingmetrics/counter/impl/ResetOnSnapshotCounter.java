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

import java.util.concurrent.atomic.AtomicLong;

/**
 * The counter which reset its state to zero after each invocation of {@link #getSum()}.
 *
 * @see SmoothlyDecayingRollingCounter
 */
class ResetOnSnapshotCounter implements WindowCounter {

    private final AtomicLong value = new AtomicLong();

    ResetOnSnapshotCounter() {
        // package-private constructor to avoid initialization without builder infrastructure
    }

    @Override
    public void add(long delta) {
        this.value.addAndGet(delta);
    }

    @Override
    synchronized public long getSum() {
        long sum = value.get();
        value.addAndGet(-sum);
        return sum;
    }

    @Override
    public String toString() {
        return "ResetOnSnapshotCounter{" +
                "value=" + value +
                '}';
    }
}
