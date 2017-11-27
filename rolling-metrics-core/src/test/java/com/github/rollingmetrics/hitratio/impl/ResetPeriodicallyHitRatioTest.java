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

public class ResetPeriodicallyHitRatioTest {

    private static int RESET_PERIOD = 1000;

    AtomicLong currentTimeMillis = new AtomicLong(0);
    Ticker ticker = Ticker.mock(currentTimeMillis);
    HitRatio hitRatio = RetentionPolicy
            .resetPeriodically(Duration.ofMillis(RESET_PERIOD))
            .withTicker(ticker)
            .newHitRatio();

    @Test
    public void shouldReturnNanWhenNothingREcorded() {
        assertEquals(Double.NaN, hitRatio.getHitRatio(), 0.0);
    }

    @Test
    public void testRegularUsage() {
        hitRatio.incrementHitCount(); // 1 - hit, 1 - total
        assertEquals(1.0, hitRatio.getHitRatio(), 0.0);

        hitRatio.incrementMissCount(); // 1 - hit, 2 - total
        assertEquals(0.5, hitRatio.getHitRatio(), 0.0);

        hitRatio.update(2, 3); // 3 - hit, 5 - total
        assertEquals(0.6, hitRatio.getHitRatio(), 0.0);

        hitRatio.update(0, 5); // 3 - hit, 10 - total
        assertEquals(0.3, hitRatio.getHitRatio(), 0.0);

        currentTimeMillis.set(RESET_PERIOD + 1);
        // state should be cleared to zero
        assertEquals(Double.NaN, hitRatio.getHitRatio(), 0.0);

        hitRatio.update(6, 10); // 3 - hit, 10 - total
        assertEquals(0.6, hitRatio.getHitRatio(), 0.0);
        currentTimeMillis.set(RESET_PERIOD * 2 + 1);
        // state should be cleared to zero
        assertEquals(Double.NaN, hitRatio.getHitRatio(), 0.0);
    }

    @Test
    public void testHandlingArithmeticOverflow() {
        hitRatio.update(Integer.MAX_VALUE / 2, Integer.MAX_VALUE);
        assertEquals(0.5, hitRatio.getHitRatio(), 0.0001);

        hitRatio.update(0, Integer.MAX_VALUE);
        assertEquals(0.25, hitRatio.getHitRatio(), 0.0001);

        hitRatio.update(Integer.MAX_VALUE, Integer.MAX_VALUE);
        assertEquals(0.625, hitRatio.getHitRatio(), 0.0001);
    }

    @Test
    public void tesIllegalApiUsageDetection() {
        HitRationTestUtil.checkIllegalApiUsageDetection(hitRatio);
    }

    @Test(timeout = 32000)
    public void testThatConcurrentThreadsNotHung() throws InterruptedException {
        HitRatio hitRatio = RetentionPolicy.resetPeriodically(Duration.ofMillis(1)).newHitRatio();
        HitRationTestUtil.runInParallel(hitRatio, TimeUnit.SECONDS.toMillis(30));
    }

}