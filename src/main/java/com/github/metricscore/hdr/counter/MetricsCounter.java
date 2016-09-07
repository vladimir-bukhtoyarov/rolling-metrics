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

import com.codahale.metrics.Counter;

import java.util.Objects;

public class MetricsCounter extends Counter {

    private final WindowCounter counter;

    public MetricsCounter(WindowCounter counter) {
        this.counter = Objects.requireNonNull(counter);
    }

    @Override
    public void inc() {
        counter.add(1);
    }

    @Override
    public void inc(long n) {
        counter.add(n);
    }

    @Override
    public void dec() {
        counter.add(-1);
    }

    @Override
    public void dec(long n) {
        counter.add(-n);
    }

    @Override
    public long getCount() {
        return counter.getSum();
    }

}
