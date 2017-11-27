/*
 *    Copyright 2017 Vladimir Bukhtoyarov
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

package com.github.rollingmetrics.counter.impl;

import com.github.rollingmetrics.counter.WindowCounter;
import com.github.rollingmetrics.retention.RetentionPolicy;
import com.github.rollingmetrics.util.Ticker;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;

public class SmoothlyDecayingRollingCounterTest {

    @Test
    public void testAddAndCalculateSum() throws Exception {
        AtomicLong timeMillis = new AtomicLong();
        Ticker ticker = Ticker.mock(timeMillis);

        WindowCounter counter = RetentionPolicy
                .resetPeriodicallyByChunks(Duration.ofSeconds(2), 2)
                .withTicker(ticker)
                .newCounter();

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

        // clear counter
        timeMillis.set(10_000);
        assertEquals(0, counter.getSum());
    }

    @Test
    public void testToString() {
        WindowCounter counter = RetentionPolicy
                .resetPeriodicallyByChunks(Duration.ofSeconds(1), 3)
                .newCounter();
        System.out.println(counter.toString());
    }

    @Test(timeout = 32000)
    public void testThatConcurrentThreadsNotHung() throws InterruptedException {
        WindowCounter counter = RetentionPolicy
                .resetPeriodicallyByChunks(Duration.ofSeconds(1), 3)
                .newCounter();
        CounterTestUtil.runInParallel(counter, TimeUnit.SECONDS.toMillis(30));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldDisallowTooShortInvalidationPeriod() {
        RetentionPolicy
                .resetPeriodicallyByChunks(Duration.ofMillis((SmoothlyDecayingRollingCounter.MIN_CHUNK_RESETTING_INTERVAL_MILLIS) - 1), 4)
                .newCounter();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldDisAllowTooManyChunk() {
        RetentionPolicy
                .resetPeriodicallyByChunks(Duration.ofSeconds(1), SmoothlyDecayingRollingCounter.MAX_CHUNKS + 1)
                .newCounter();
    }

}