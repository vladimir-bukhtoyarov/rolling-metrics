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

package com.github.rollingmetrics.histogram.hdr.impl;


import com.github.rollingmetrics.histogram.hdr.RollingHdrHistogram;
import com.github.rollingmetrics.histogram.hdr.RollingSnapshot;
import com.github.rollingmetrics.util.Clock;
import com.github.rollingmetrics.util.MockExecutor;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static junit.framework.TestCase.assertEquals;

public class ResetPeriodicallyRollingHdrHistogramImplTest {

    @Test
    public void test() {
        AtomicLong time = new AtomicLong(0);
        Clock wallClock = Clock.mock(time);
        RollingHdrHistogram histogram = RollingHdrHistogram.builder()
                .withClock(wallClock)
                .resetReservoirPeriodically(Duration.ofMillis(1000))
                .withBackgroundExecutor(MockExecutor.INSTANCE)
                .build();

        histogram.update(10);
        histogram.update(20);
        RollingSnapshot snapshot = histogram.getSnapshot();
        assertEquals(10, snapshot.getMin());
        assertEquals(20, snapshot.getMax());

        time.getAndAdd(900); // 900
        histogram.update(30);
        histogram.update(40);
        snapshot = histogram.getSnapshot();
        assertEquals(10, snapshot.getMin());
        assertEquals(40, snapshot.getMax());

        time.getAndAdd(99); // 999
        histogram.update(8);
        histogram.update(60);
        snapshot = histogram.getSnapshot();
        assertEquals(8, snapshot.getMin());
        assertEquals(60, snapshot.getMax());

        time.getAndAdd(1); // 1000
        histogram.update(70);
        histogram.update(80);
        snapshot = histogram.getSnapshot();
        assertEquals(70, snapshot.getMin());
        assertEquals(80, snapshot.getMax());

        time.getAndAdd(1001); // 2001
        histogram.update(90);
        histogram.update(100);
        snapshot = histogram.getSnapshot();
        assertEquals(90, snapshot.getMin());
        assertEquals(100, snapshot.getMax());

        time.getAndAdd(1000); // 3001
        snapshot = histogram.getSnapshot();
        assertEquals(0, snapshot.getMin());
        assertEquals(0, snapshot.getMax());

        time.getAndAdd(1); // 3002
        histogram.update(42);
        snapshot = histogram.getSnapshot();
        assertEquals(42, snapshot.getMin());
        assertEquals(42, snapshot.getMax());

        time.getAndAdd(2000); // 5002
        snapshot = histogram.getSnapshot();
        assertEquals(0, snapshot.getMin());
        assertEquals(0, snapshot.getMax());
    }

    @Test
    public void testToString() {
        RollingHdrHistogram.builder()
                .resetReservoirPeriodically(Duration.ofSeconds(1))
                .build().toString();
    }

    @Test(timeout = 32000)
    public void testThatConcurrentThreadsNotHung() throws InterruptedException {
        RollingHdrHistogram histogram = RollingHdrHistogram.builder()
                .resetReservoirPeriodically(Duration.ofSeconds(1))
                .build();

        HistogramTestUtil.runInParallel(histogram, TimeUnit.SECONDS.toMillis(30));
    }

}
