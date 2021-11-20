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
import com.github.rollingmetrics.util.Ticker;
import com.github.rollingmetrics.util.MockExecutor;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static junit.framework.TestCase.assertEquals;

public class ResetByChunksRollingHdrHistogramImplTest {

    @Test
    public void test() {
        AtomicLong time = new AtomicLong(0);
        Ticker ticker = Ticker.mock(time);
        RollingHdrHistogram histogram = RollingHdrHistogram.builder()
                .withTicker(ticker)
                .resetReservoirPeriodicallyByChunks(Duration.ofMillis(3000), 3)
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
        histogram.update(9);
        histogram.update(60);
        snapshot = histogram.getSnapshot();
        assertEquals(9, snapshot.getMin());
        assertEquals(60, snapshot.getMax());

        time.getAndAdd(1); // 1000
        histogram.update(12);
        histogram.update(70);
        snapshot = histogram.getSnapshot();
        assertEquals(9, snapshot.getMin());
        assertEquals(70, snapshot.getMax());

        time.getAndAdd(1001); // 2001
        histogram.update(13);
        histogram.update(80);
        snapshot = histogram.getSnapshot();
        assertEquals(9, snapshot.getMin());
        assertEquals(80, snapshot.getMax());

        time.getAndAdd(1000); // 3001
        snapshot = histogram.getSnapshot();
        assertEquals(9, snapshot.getMin());
        assertEquals(80, snapshot.getMax());

        time.getAndAdd(999); // 4000
        snapshot = histogram.getSnapshot();
        assertEquals(12, snapshot.getMin());
        assertEquals(80, snapshot.getMax());
        histogram.update(1);
        histogram.update(200);
        snapshot = histogram.getSnapshot();
        assertEquals(1, snapshot.getMin());
        assertEquals(200, snapshot.getMax());

        time.getAndAdd(10000); // 14000
        snapshot = histogram.getSnapshot();
        assertEquals(0, snapshot.getMin());
        assertEquals(0, snapshot.getMax());
        histogram.update(3);

        time.addAndGet(3999); // 17999
        snapshot = histogram.getSnapshot();
        assertEquals(3, snapshot.getMax());

        time.addAndGet(1); // 18000
        snapshot = histogram.getSnapshot();
        assertEquals(0, snapshot.getMax());
    }

    @Test
    public void testToString() {
        RollingHdrHistogram.builder()
                .resetReservoirPeriodicallyByChunks(Duration.ofSeconds(60), 3)
                .build().toString();
    }

    @Test(timeout = 32000)
    public void testThatConcurrentThreadsNotHungWithThreeChunks() throws InterruptedException {
        RollingHdrHistogram histogram = RollingHdrHistogram.builder()
                .resetReservoirPeriodicallyByChunks(Duration.ofSeconds(3), 3)
                .build();

        HistogramTestUtil.runInParallel(histogram, TimeUnit.SECONDS.toMillis(30));
    }

    @Test
    public void testIsolationOfFullSnapshot() {
        RollingHdrHistogram histogram = RollingHdrHistogram.builder()
                .withoutSnapshotOptimization()
                .resetReservoirPeriodicallyByChunks(Duration.ofSeconds(60), 3)
                .build();

        histogram.update(13);
        RollingSnapshot snapshot1 = histogram.getSnapshot();

        histogram.update(42);
        RollingSnapshot snapshot2 = histogram.getSnapshot();

        assertEquals(13, snapshot1.getMax());
        assertEquals(42, snapshot2.getMax());

        assertEquals( 13, snapshot1.getMin());
        assertEquals(13, snapshot2.getMin());

        assertEquals(1, snapshot1.getSamplesCount());
        assertEquals(2, snapshot2.getSamplesCount());
    }

}