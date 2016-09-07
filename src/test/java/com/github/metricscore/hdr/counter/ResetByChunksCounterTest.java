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

import com.codahale.metrics.Clock;
import com.github.metricscore.hdr.ChunkEvictionPolicy;
import com.github.metricscore.hdr.MockClock;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;

public class ResetByChunksCounterTest {

    @Test
    public void testSmoothlyInvalidateOldestChunk() throws Exception {
        AtomicLong timeMillis = new AtomicLong();
        Clock clock = MockClock.mock(timeMillis);
        ChunkEvictionPolicy evictionPolicy = new ChunkEvictionPolicy(Duration.ofSeconds(1), 3, true, true);
        ResetByChunksCounter counter = new ResetByChunksCounter(evictionPolicy, clock);

        counter.add(100);
        assertEquals(100, counter.getSum());

        timeMillis.set(2600);
        assertEquals(40, counter.getSum());

        timeMillis.set(2980);
        assertEquals(2, counter.getSum());

        timeMillis.set(3000);
        assertEquals(0, counter.getSum());

        counter.add(200);
        assertEquals(200, counter.getSum());

        timeMillis.set(4000);
        assertEquals(200, counter.getSum());

        timeMillis.set(5000);
        assertEquals(200, counter.getSum());
        counter.add(300);
        assertEquals(500, counter.getSum());

        timeMillis.set(5500);
        assertEquals(400, counter.getSum());

        timeMillis.set(6000);
        assertEquals(300, counter.getSum());
    }

    @Test
    public void testSkipUncompletedChunkToSnapshot() throws Exception {
        AtomicLong timeMillis = new AtomicLong();
        Clock clock = MockClock.mock(timeMillis);
        ChunkEvictionPolicy evictionPolicy = new ChunkEvictionPolicy(Duration.ofSeconds(1), 3, false, false);
        ResetByChunksCounter counter = new ResetByChunksCounter(evictionPolicy, clock);

        counter.add(100);
        assertEquals(0, counter.getSum());

        timeMillis.set(999);
        counter.add(300);
        assertEquals(0, counter.getSum());

        timeMillis.set(1000);
        assertEquals(400, counter.getSum());

        timeMillis.set(1001);
        counter.add(500);
        assertEquals(400, counter.getSum());

        timeMillis.set(1999);
        assertEquals(400, counter.getSum());

        timeMillis.set(2000);
        assertEquals(900, counter.getSum());
    }

    @Test
    public void testSkipUncompletedChunkToSnapshotAndInvalidateOldestChunkDiscretely() throws Exception {
        AtomicLong timeMillis = new AtomicLong();
        Clock clock = MockClock.mock(timeMillis);
        ChunkEvictionPolicy evictionPolicy = new ChunkEvictionPolicy(Duration.ofSeconds(1), 3, true, false);
        ResetByChunksCounter counter = new ResetByChunksCounter(evictionPolicy, clock);

        counter.add(100);
        assertEquals(100, counter.getSum());

        timeMillis.set(2600);
        assertEquals(100, counter.getSum());

        timeMillis.set(2980);
        assertEquals(100, counter.getSum());

        timeMillis.set(3000);
        assertEquals(0, counter.getSum());

        counter.add(200);
        assertEquals(200, counter.getSum());

        timeMillis.set(4000);
        assertEquals(200, counter.getSum());

        timeMillis.set(5000);
        assertEquals(200, counter.getSum());
        counter.add(300);
        assertEquals(500, counter.getSum());

        timeMillis.set(5500);
        assertEquals(500, counter.getSum());

        timeMillis.set(6000);
        assertEquals(300, counter.getSum());
    }

    @Test
    public void testToString() {
        ChunkEvictionPolicy evictionPolicy = new ChunkEvictionPolicy(Duration.ofSeconds(1), 3, true, false);
        System.out.println(WindowCounter.newResetByChunkCounter(evictionPolicy).toString());
    }

    @Test(timeout = 32000)
    public void testThatConcurrentThreadsNotHungWithThreeChunks() throws InterruptedException {
        ChunkEvictionPolicy evictionPolicy = new ChunkEvictionPolicy(Duration.ofSeconds(1), 3, true, false);
        WindowCounter counter = WindowCounter.newResetByChunkCounter(evictionPolicy);
        CounterTestUtil.runInParallel(counter, Duration.ofSeconds(30));
    }

    @Test(timeout = 32000)
    public void testThatConcurrentThreadsNotHungWithFourChunks() throws InterruptedException {
        ChunkEvictionPolicy evictionPolicy = new ChunkEvictionPolicy(Duration.ofSeconds(1), 4, true, false);
        WindowCounter counter = WindowCounter.newResetByChunkCounter(evictionPolicy);
        CounterTestUtil.runInParallel(counter, Duration.ofSeconds(30));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldDisallowTooShortInvalidationPeriod() {
        ChunkEvictionPolicy evictionPolicy = new ChunkEvictionPolicy(Duration.ofMillis((ResetByChunksCounter.MIN_CHUNK_RESETTING_INTERVAL_MILLIS) - 1), 4);
        WindowCounter.newResetByChunkCounter(evictionPolicy);
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeValuesShouldBeDisallowed() {
        ChunkEvictionPolicy evictionPolicy = new ChunkEvictionPolicy(Duration.ofMillis((ResetByChunksCounter.MIN_CHUNK_RESETTING_INTERVAL_MILLIS) - 1), 4);
        WindowCounter.newResetByChunkCounter(evictionPolicy).add(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void zeroValuesShouldBeDisallowed() {
        ChunkEvictionPolicy evictionPolicy = new ChunkEvictionPolicy(Duration.ofMillis((ResetByChunksCounter.MIN_CHUNK_RESETTING_INTERVAL_MILLIS) - 1), 4);
        WindowCounter.newResetByChunkCounter(evictionPolicy).add(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldDisAllowTooManyChunk() {
        ChunkEvictionPolicy evictionPolicy = new ChunkEvictionPolicy(Duration.ofSeconds(1), ResetByChunksCounter.MAX_CHUNKS + 1);
        WindowCounter.newResetByChunkCounter(evictionPolicy);
    }

}