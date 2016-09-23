/*
 *    Copyright 2016 Vladimir Bukhtoyarov
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

package com.github.metricscore.hdr.hitratio;

import org.junit.Test;

import java.time.Duration;

import static org.junit.Assert.assertEquals;

/**
 * Created by vladimir.bukhtoyarov on 23.09.2016.
 */
public class ResetOnSnapshotHitRatioTest {

    HitRatio hitRatio = new ResetOnSnapshotHitRatio();

    @Test
    public void shouldReturnNanWhenNothingREcorded() {
        assertEquals(Double.NaN, hitRatio.getHitRatio(), 0.0);
    }

    @Test
    public void testRegularUsage() {
        hitRatio.incrementHitCount(); // 1 - hit, 1 - total
        hitRatio.incrementMissCount(); // 1 - hit, 2 - total
        hitRatio.update(2, 3); // 3 - hit, 5 - total
        hitRatio.update(0, 5); // 3 - hit, 10 - total
        assertEquals(0.3, hitRatio.getHitRatio(), 0.0); // 0 - hit, 0 - total
        assertEquals(Double.NaN, hitRatio.getHitRatio(), 0.0); // 0 - hit, 0 - total

        hitRatio.update(2, 100); // 2 - hit, 100 - total
        assertEquals(0.02, hitRatio.getHitRatio(), 0.0); // 0 - hit, 0 - total
        assertEquals(Double.NaN, hitRatio.getHitRatio(), 0.0); // 0 - hit, 0 - total
    }

    @Test
    public void testHandlingArithmeticOverflow() {
        hitRatio.update(Integer.MAX_VALUE / 2, Integer.MAX_VALUE);
        hitRatio.update(0, Integer.MAX_VALUE);
        hitRatio.update(Integer.MAX_VALUE, Integer.MAX_VALUE);
        assertEquals(0.625, hitRatio.getHitRatio(), 0.0001);
        assertEquals(Double.NaN, hitRatio.getHitRatio(), 0.0);
    }

    @Test
    public void tesIllegalApiUsageDetection() {
        HitRationTestUtil.checkIllegalApiUsageDetection(hitRatio);
    }

    @Test(timeout = 32000)
    public void testThatConcurrentThreadsNotHung() throws InterruptedException {
        HitRationTestUtil.runInParallel(hitRatio, Duration.ofSeconds(30));
    }

}