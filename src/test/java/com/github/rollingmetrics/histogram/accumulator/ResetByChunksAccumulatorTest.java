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

package com.github.rollingmetrics.histogram.accumulator;

import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Snapshot;
import com.github.rollingmetrics.util.Clock;
import com.github.rollingmetrics.histogram.HdrBuilder;
import com.github.rollingmetrics.util.MockExecutor;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static junit.framework.TestCase.assertEquals;

public class ResetByChunksAccumulatorTest {

    @Test
    public void test() {
        AtomicLong time = new AtomicLong(0);
        Clock wallClock = Clock.mock(time);
        Reservoir reservoir = new HdrBuilder(wallClock)
                .resetReservoirPeriodicallyByChunks(Duration.ofMillis(3000), 3)
                .withBackgroundExecutor(MockExecutor.INSTANCE)
                .buildReservoir();

        reservoir.update(10);
        reservoir.update(20);
        Snapshot snapshot = reservoir.getSnapshot();
        assertEquals(10, snapshot.getMin());
        assertEquals(20, snapshot.getMax());

        time.getAndAdd(900); // 900
        reservoir.update(30);
        reservoir.update(40);
        snapshot = reservoir.getSnapshot();
        assertEquals(10, snapshot.getMin());
        assertEquals(40, snapshot.getMax());

        time.getAndAdd(99); // 999
        reservoir.update(9);
        reservoir.update(60);
        snapshot = reservoir.getSnapshot();
        assertEquals(9, snapshot.getMin());
        assertEquals(60, snapshot.getMax());

        time.getAndAdd(1); // 1000
        reservoir.update(12);
        reservoir.update(70);
        snapshot = reservoir.getSnapshot();
        assertEquals(9, snapshot.getMin());
        assertEquals(70, snapshot.getMax());

        time.getAndAdd(1001); // 2001
        reservoir.update(13);
        reservoir.update(80);
        snapshot = reservoir.getSnapshot();
        assertEquals(9, snapshot.getMin());
        assertEquals(80, snapshot.getMax());

        time.getAndAdd(1000); // 3001
        snapshot = reservoir.getSnapshot();
        assertEquals(9, snapshot.getMin());
        assertEquals(80, snapshot.getMax());

        time.getAndAdd(999); // 4000
        snapshot = reservoir.getSnapshot();
        assertEquals(12, snapshot.getMin());
        assertEquals(80, snapshot.getMax());
        reservoir.update(1);
        reservoir.update(200);
        snapshot = reservoir.getSnapshot();
        assertEquals(1, snapshot.getMin());
        assertEquals(200, snapshot.getMax());

        time.getAndAdd(10000); // 14000
        snapshot = reservoir.getSnapshot();
        assertEquals(0, snapshot.getMin());
        assertEquals(0, snapshot.getMax());
        reservoir.update(3);

        time.addAndGet(3999); // 17999
        snapshot = reservoir.getSnapshot();
        assertEquals(3, snapshot.getMax());

        time.addAndGet(1); // 18000
        snapshot = reservoir.getSnapshot();
        assertEquals(0, snapshot.getMax());
    }

    @Test
    public void testToString() {
        new HdrBuilder().resetReservoirPeriodicallyByChunks(Duration.ofSeconds(60), 3)
                .buildReservoir().toString();
    }

    @Test(timeout = 32000)
    public void testThatConcurrentThreadsNotHungWithThreeChunks() throws InterruptedException {
        Reservoir reservoir = new HdrBuilder()
                .resetReservoirPeriodicallyByChunks(Duration.ofSeconds(3), 3)
                .buildReservoir();

        HistogramUtil.runInParallel(reservoir, TimeUnit.SECONDS.toMillis(30));
    }

}