/*
 *
 *  Copyright 2017 Vladimir Bukhtoyarov
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

package com.github.rollingmetrics.adapter;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;


public class GaugeToCounterAdapterTest {

    AtomicLong value = new AtomicLong();
    GaugeToCounterAdapter adapter = new GaugeToCounterAdapter(() -> value.get());

    @Test
    public void getCount() throws Exception {
        assertEquals(0L, adapter.getCount());

        value.incrementAndGet();
        assertEquals(1L, adapter.getCount());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void inc() throws Exception {
        adapter.inc();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void inc1() throws Exception {
        adapter.inc(2);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void dec() throws Exception {
        adapter.dec();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void dec1() throws Exception {
        adapter.dec(33);
    }

}