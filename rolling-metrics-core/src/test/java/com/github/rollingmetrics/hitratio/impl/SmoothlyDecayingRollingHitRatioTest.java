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

package com.github.rollingmetrics.hitratio.impl;

import com.github.rollingmetrics.hitratio.HitRatio;
import com.github.rollingmetrics.retention.RetentionPolicy;
import com.github.rollingmetrics.util.Ticker;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;


public class SmoothlyDecayingRollingHitRatioTest {

    private static int ROLLING_TIME_WINDOW_MILLIS = 5_000;
    private static int CHUNK_COUNT = 5;

    AtomicLong currentTimeMillis = new AtomicLong(0);
    Ticker ticker = Ticker.mock(currentTimeMillis);
    HitRatio hitRatio = RetentionPolicy
            .resetPeriodicallyByChunks(Duration.ofMillis(ROLLING_TIME_WINDOW_MILLIS), CHUNK_COUNT)
            .withTicker(ticker)
            .newHitRatio();

    @Test
    public void testChunkRotation() {
        hitRatio.update(100, 100);
        assertEquals(1.0, hitRatio.getHitRatio(), 0.001);

        // switch to second chunk
        currentTimeMillis.set(1000);
        hitRatio.update(80, 100);
        assertEquals(0.9, hitRatio.getHitRatio(), 0.001);

        // switch to third chunk
        currentTimeMillis.set(2000);
        hitRatio.update(60, 100);
        assertEquals(0.8, hitRatio.getHitRatio(), 0.001);

        // switch to fourth chunk
        currentTimeMillis.set(3000);
        hitRatio.update(60, 100);
        assertEquals(0.75, hitRatio.getHitRatio(), 0.001);

        // switch to fifth chunk
        currentTimeMillis.set(4000);
        hitRatio.update(10, 100);
        assertEquals(0.62, hitRatio.getHitRatio(), 0.001);

        // switch to sixth chunk
        currentTimeMillis.set(5000);
        assertEquals(0.62, hitRatio.getHitRatio(), 0.001);

        currentTimeMillis.set(6000);
        // data of first chunk should be evicted
        assertEquals(0.525, hitRatio.getHitRatio(), 0.001);

        currentTimeMillis.set(7000);
        // data of second chunk should be evicted
        assertEquals(0.433, hitRatio.getHitRatio(), 0.001);

        currentTimeMillis.set(8000);
        // data of third chunk should be evicted
        assertEquals(0.35, hitRatio.getHitRatio(), 0.001);

        currentTimeMillis.set(9000);
        // data of fourth chunk should be evicted
        assertEquals(0.1, hitRatio.getHitRatio(), 0.001);

        currentTimeMillis.set(10_000);
        // data of fifth chunk should be evicted
        assertEquals(Double.NaN, hitRatio.getHitRatio(), 0.001);

        hitRatio.update(90, 1000);
        assertEquals(0.09, hitRatio.getHitRatio(), 0.001);
    }

    @Test
    public void testSmoothlyEvictionFromOldestChunk() {
        hitRatio.update(50, 100);
        assertEquals(0.5, hitRatio.getHitRatio(), 0.001);

        currentTimeMillis.set(1_000);
        hitRatio.update(100, 100);
        assertEquals(0.75, hitRatio.getHitRatio(), 0.001);

        currentTimeMillis.set(5_500);
        // oldest chunk should lost 50% of its weight
        assertEquals(0.833, hitRatio.getHitRatio(), 0.001);

        currentTimeMillis.set(5_750);
        // oldest chunk should lost 75% of its weight
        assertEquals(0.896, hitRatio.getHitRatio(), 0.001);

        currentTimeMillis.set(6_000);
        // oldest chunk should be fully invalidated
        assertEquals(1.0, hitRatio.getHitRatio(), 0.001);
    }

    @Test
    public void testHandlingArithmeticOverflow() {
        hitRatio.update(Integer.MAX_VALUE / 2, Integer.MAX_VALUE);
        assertEquals(0.5, hitRatio.getHitRatio(), 0.0001);

        hitRatio.update(0, Integer.MAX_VALUE);
        assertEquals(0.25, hitRatio.getHitRatio(), 0.0001);

        currentTimeMillis.set(1000);
        hitRatio.update(Integer.MAX_VALUE / 2, Integer.MAX_VALUE);
        assertEquals(0.375, hitRatio.getHitRatio(), 0.0001);

        hitRatio.update(0, Integer.MAX_VALUE);
        assertEquals(0.25, hitRatio.getHitRatio(), 0.0001);
    }

    @Test
    public void tesIllegalApiUsageDetection() {
        HitRationTestUtil.checkIllegalApiUsageDetection(hitRatio);
    }

    @Test(expected = IllegalArgumentException.class)
    public void tooShortTimeWindowShouldBeDisallowed() {
        RetentionPolicy
                .resetPeriodicallyByChunks(Duration.ofMillis(SmoothlyDecayingRollingHitRatio.MIN_CHUNK_RESETTING_INTERVAL_MILLIS - 1), 5)
                .newHitRatio();
    }

    @Test(expected = IllegalArgumentException.class)
    public void tooManyChunksShouldBeDisallowed() {
        RetentionPolicy
                .resetPeriodicallyByChunks(Duration.ofMinutes(1), SmoothlyDecayingRollingHitRatio.MAX_CHUNKS + 1)
                .newHitRatio();
    }

    @Test
    public void testToString() throws Exception {
        HitRatio hitRatio = RetentionPolicy
                .resetPeriodicallyByChunks(Duration.ofMinutes(1), 6)
                .newHitRatio();
        System.out.println(hitRatio.toString());
    }

    @Test(timeout = 32000)
    public void testThatConcurrentThreadsNotHung() throws InterruptedException {
        HitRatio hitRatio = RetentionPolicy
                .resetPeriodicallyByChunks(Duration.ofSeconds(1), 10)
                .newHitRatio();
        HitRationTestUtil.runInParallel(hitRatio, TimeUnit.SECONDS.toMillis(30));
    }

}