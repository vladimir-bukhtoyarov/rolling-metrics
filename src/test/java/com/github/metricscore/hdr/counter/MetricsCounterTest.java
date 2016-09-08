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

import org.junit.Test;

import static org.junit.Assert.*;

public class MetricsCounterTest {

    WindowCounter windowCounter = new ResetAtSnapshotCounter();
    MetricsCounter counter = new MetricsCounter(windowCounter);

    @Test
    public void incByOne() throws Exception {
        counter.inc();
        assertEquals(1L, counter.getCount());
    }

    @Test
    public void inc() throws Exception {
        counter.inc(42);
        assertEquals(42L, counter.getCount());
    }

    @Test
    public void decByOne() throws Exception {
        counter.dec();
        counter.dec();
        assertEquals(-2L, counter.getCount());
    }

    @Test
    public void dec() throws Exception {
        counter.dec(42L);
        assertEquals(-42L, counter.getCount());
    }

}