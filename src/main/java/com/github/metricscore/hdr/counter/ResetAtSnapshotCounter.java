/*
 *    Copyright 2016 Vladimir Bukhtoyarov
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

package com.github.metricscore.hdr.counter;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by vladimir.bukhtoyarov on 02.09.2016.
 */
class ResetAtSnapshotCounter implements WindowCounter {

    private final AtomicLong value = new AtomicLong();

    public ResetAtSnapshotCounter() {
        super();
    }

    @Override
    public void add(long delta) {
        if (delta < 1) {
            throw new IllegalArgumentException("delta should be >= 1");
        }
        this.value.addAndGet(delta);
    }

    @Override
    synchronized public Long getValue() {
        long sum = value.get();
        value.addAndGet(-sum);
        return sum;
    }

}
