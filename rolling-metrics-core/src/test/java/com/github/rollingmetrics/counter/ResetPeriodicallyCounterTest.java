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

package com.github.rollingmetrics.counter;

import com.github.rollingmetrics.util.Ticker;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;

public class ResetPeriodicallyCounterTest {

    @Test(expected = IllegalArgumentException.class)
    public void negativeResettingDurationShouldBeDisallowed() {
        new ResetPeriodicallyCounter(Duration.ofSeconds(-1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void zeroResettingDurationShouldBeDisallowed() {
        new ResetPeriodicallyCounter(Duration.ZERO);
    }

    @Test
    public void testRotation() {
        AtomicLong timeMillis = new AtomicLong();
        Ticker ticker = Ticker.mock(timeMillis);
        WindowCounter counter = new ResetPeriodicallyCounter(Duration.ofMillis(1000), ticker);

        counter.add(100);
        assertEquals(100, counter.getSum());

        timeMillis.set(500);
        counter.add(200);
        assertEquals(300, counter.getSum());

        timeMillis.set(999);
        assertEquals(300, counter.getSum());

        timeMillis.set(1000);
        assertEquals(0, counter.getSum());

        timeMillis.set(1500);
        counter.add(444);

        timeMillis.set(2100);
        assertEquals(0, counter.getSum());
    }

    @Test
    public void testToString() {
        System.out.println(new ResetPeriodicallyCounter(Duration.ofMillis(1000)).toString());
    }

    @Test(timeout = 32000)
    public void testThatConcurrentThreadsNotHung() throws InterruptedException {
        WindowCounter counter = new ResetPeriodicallyCounter(Duration.ofMillis(50));
        CounterTestUtil.runInParallel(counter, TimeUnit.SECONDS.toMillis(30));
    }


}