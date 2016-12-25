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

package com.github.rollingmetrics.counter;

import java.util.concurrent.atomic.AtomicLong;

/**
 * The counter which reset its state to zero after each invocation of {@link #getSum()}.
 *
 * <p>
 * Concurrency properties:
 * <ul>
 *     <li>Writing is lock-free. Writers do not block writers and readers.</li>
 *     <li>Sum reading always happen inside synchronized block, so readers block each other, but readers never block writers.</li>
 * </ul>
 *
 * <p>
 * Usage recommendations:
 * <ul>
 *     <li>When you do not need in "rolling time window" semantic. Else use {@link SmoothlyDecayingRollingCounter}</li>
 *     <li>When you need in 100 percents guarantee that one measure can not be reported twice.</li>
 *     <li>Only if one kind of reader interests in value of counter.
 *     Usage of this implementation for case of multiple readers will be a bad idea because of readers will steal data from each other.
 *     </li>
 * </ul>
 *
 * @see SmoothlyDecayingRollingCounter
 */
public class ResetOnSnapshotCounter implements WindowCounter {

    private final AtomicLong value = new AtomicLong();

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
