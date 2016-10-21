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
import com.codahale.metrics.Gauge;

import java.util.Objects;

/**
 * Adapter for gauge which has a counter nature but is not counter by itself.
 */
public class GaugeToCounterAdapter extends Counter {

    private final Gauge<Number> gauge;

    public GaugeToCounterAdapter(Gauge<Number> gauge) {
        this.gauge = Objects.requireNonNull(gauge);
    }

    @Override
    public long getCount() {
        return gauge.getValue().longValue();
    }

    @Override
    public void inc() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void inc(long n) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void dec() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void dec(long n) {
        throw new UnsupportedOperationException();
    }

}
