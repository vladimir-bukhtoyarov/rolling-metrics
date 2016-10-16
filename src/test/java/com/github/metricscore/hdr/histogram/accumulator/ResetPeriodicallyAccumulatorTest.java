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

package com.github.metricscore.hdr.histogram.accumulator;


import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Snapshot;
import com.github.metricscore.hdr.util.Clock;
import com.github.metricscore.hdr.histogram.HdrBuilder;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static junit.framework.TestCase.assertEquals;

public class ResetPeriodicallyAccumulatorTest {

    @Test
    public void test() {
        AtomicLong time = new AtomicLong(System.currentTimeMillis());
        Clock wallClock = Clock.mock(time);
        Reservoir reservoir = new HdrBuilder(wallClock)
                .resetReservoirPeriodically(Duration.ofMillis(1000))
                .buildReservoir();

        reservoir.update(10);
        reservoir.update(20);
        Snapshot snapshot = reservoir.getSnapshot();
        assertEquals(10, snapshot.getMin());
        assertEquals(20, snapshot.getMax());

        time.getAndAdd(900);
        reservoir.update(30);
        reservoir.update(40);
        snapshot = reservoir.getSnapshot();
        assertEquals(10, snapshot.getMin());
        assertEquals(40, snapshot.getMax());

        time.getAndAdd(99);
        reservoir.update(8);
        reservoir.update(60);
        snapshot = reservoir.getSnapshot();
        assertEquals(8, snapshot.getMin());
        assertEquals(60, snapshot.getMax());

        time.getAndAdd(1);
        reservoir.update(70);
        reservoir.update(80);
        snapshot = reservoir.getSnapshot();
        assertEquals(70, snapshot.getMin());
        assertEquals(80, snapshot.getMax());

        time.getAndAdd(1001);
        reservoir.update(90);
        reservoir.update(100);
        snapshot = reservoir.getSnapshot();
        assertEquals(90, snapshot.getMin());
        assertEquals(100, snapshot.getMax());

        time.getAndAdd(1000);
        snapshot = reservoir.getSnapshot();
        assertEquals(0, snapshot.getMin());
        assertEquals(0, snapshot.getMax());

        time.getAndAdd(1);
        reservoir.update(42);
        snapshot = reservoir.getSnapshot();
        assertEquals(42, snapshot.getMin());
        assertEquals(42, snapshot.getMax());

        time.getAndAdd(2000);
        snapshot = reservoir.getSnapshot();
        assertEquals(0, snapshot.getMin());
        assertEquals(0, snapshot.getMax());
    }

    @Test
    public void testToString() {
        new HdrBuilder().resetReservoirPeriodically(Duration.ofSeconds(1)).buildReservoir().toString();
    }

    @Test(timeout = 12000)
    public void testThatConcurrentThreadsNotHung() throws InterruptedException {
        Reservoir reservoir = new HdrBuilder()
                .resetReservoirPeriodically(Duration.ofSeconds(1))
                .buildReservoir();

        HistogramUtil.runInParallel(reservoir, Duration.ofSeconds(10));
    }

}
