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

package com.github.metricscore.hdrhistogram.accumulator;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Snapshot;
import com.github.metricscore.hdrhistogram.HdrBuilder;
import com.github.metricscore.hdrhistogram.MockClock;
import org.junit.Test;

import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static junit.framework.TestCase.assertEquals;

public class ResetByChunksAccumulatorTest {

    @Test
    public void testOneChunk() {
        AtomicLong time = new AtomicLong(System.currentTimeMillis());
        Clock wallClock = MockClock.mock(time);
        Reservoir reservoir = new HdrBuilder(wallClock)
                .resetReservoirPeriodically(Duration.ofMillis(1000))
                .buildReservoir();

        reservoir.update(10);
        reservoir.update(20);
        Snapshot firstSnapshot = reservoir.getSnapshot();
        assertEquals(10, firstSnapshot.getMin());
        assertEquals(20, firstSnapshot.getMax());

        time.getAndAdd(900);
        reservoir.update(30);
        reservoir.update(40);
        Snapshot secondSnapshot = reservoir.getSnapshot();
        assertEquals(10, secondSnapshot.getMin());
        assertEquals(40, secondSnapshot.getMax());

        time.getAndAdd(99);
        reservoir.update(8);
        reservoir.update(60);
        Snapshot thirdSnapshot = reservoir.getSnapshot();
        assertEquals(8, thirdSnapshot.getMin());
        assertEquals(60, thirdSnapshot.getMax());

        time.getAndAdd(1);
        reservoir.update(70);
        reservoir.update(80);
        Snapshot firstNewSnapshot = reservoir.getSnapshot();
        assertEquals(70, firstNewSnapshot.getMin());
        assertEquals(80, firstNewSnapshot.getMax());

        time.getAndAdd(1001);
        reservoir.update(90);
        reservoir.update(100);
        Snapshot secondNewSnapshot = reservoir.getSnapshot();
        assertEquals(90, secondNewSnapshot.getMin());
        assertEquals(100, secondNewSnapshot.getMax());
    }

    @Test
    public void testThreeChunks() {
        AtomicLong time = new AtomicLong(1000);
        Clock wallClock = MockClock.mock(time);
        Reservoir reservoir = new HdrBuilder(wallClock)
                .resetReservoirByChunks(Duration.ofMillis(1000), 3)
                .buildReservoir();

        reservoir.update(10);
        reservoir.update(20);
        Snapshot snapshot = reservoir.getSnapshot();
        assertEquals(10, snapshot.getMin());
        assertEquals(20, snapshot.getMax());

        time.getAndAdd(900); // 1900
        reservoir.update(30);
        reservoir.update(40);
        snapshot = reservoir.getSnapshot();
        assertEquals(10, snapshot.getMin());
        assertEquals(40, snapshot.getMax());

        time.getAndAdd(99); // 1999
        reservoir.update(9);
        reservoir.update(60);
        snapshot = reservoir.getSnapshot();
        assertEquals(9, snapshot.getMin());
        assertEquals(60, snapshot.getMax());

        // should be switched to second chunk
        time.getAndAdd(1); // 2000
        reservoir.update(12);
        reservoir.update(70);
        snapshot = reservoir.getSnapshot();
        assertEquals(9, snapshot.getMin());
        assertEquals(70, snapshot.getMax());

        // should be switched to third chunk
        time.getAndAdd(1001); // 3001
        reservoir.update(13);
        reservoir.update(80);
        snapshot = reservoir.getSnapshot();
        assertEquals(9, snapshot.getMin());
        assertEquals(80, snapshot.getMax());

        // should be switched to first chunk(right phase)
        time.getAndAdd(1000); // 4001
        snapshot = reservoir.getSnapshot();
        assertEquals(12, snapshot.getMin());
        assertEquals(80, snapshot.getMax());

        // should be switched to second chunk(right phase)
        time.getAndAdd(999); // 5000
        snapshot = reservoir.getSnapshot();
        assertEquals(13, snapshot.getMin());
        assertEquals(80, snapshot.getMax());
        reservoir.update(1);
        reservoir.update(200);
        snapshot = reservoir.getSnapshot();
        assertEquals(1, snapshot.getMin());
        assertEquals(200, snapshot.getMax());

        // should invalidate all measures
        time.getAndAdd(10000); // 15000
        snapshot = reservoir.getSnapshot();
        assertEquals(0, snapshot.getMin());
        assertEquals(0, snapshot.getMax());
    }

    @Test(timeout = 12000)
    public void testThatConcurrentThreadsNotHungWithOneChunk() throws InterruptedException {
        Reservoir reservoir = new HdrBuilder()
                .resetReservoirPeriodically(Duration.ofSeconds(1))
                .buildReservoir();

        runInParallel(reservoir, Duration.ofSeconds(10));
    }

    @Test(timeout = 12000)
    public void testThatConcurrentThreadsNotHungWithFourChunk() throws InterruptedException {
        Reservoir reservoir = new HdrBuilder()
                .resetReservoirByChunks(Duration.ofSeconds(1), 4)
                .buildReservoir();

        runInParallel(reservoir, Duration.ofSeconds(10));
    }

    private void runInParallel(Reservoir reservoir, Duration duration) throws InterruptedException {
        AtomicBoolean stopFlag = new AtomicBoolean(false);

        // let concurrent threads to work fo 3 seconds
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                stopFlag.set(true);
            }
        }, duration.toMillis());

        Thread[] threads = new Thread[Runtime.getRuntime().availableProcessors() * 2];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                try {
                    // update reservoir 100 times and take snapshot on each cycle
                    while (!stopFlag.get()) {
                        for (int j = 1; j <= 100; j++) {
                            reservoir.update(ThreadLocalRandom.current().nextInt(j));
                        }
                        reservoir.getSnapshot();
                    }
                } catch (Exception e){
                    e.printStackTrace();
                }
            });
            threads[i].start();
        }
        for (Thread thread: threads) {
            thread.join();
        }
    }

}