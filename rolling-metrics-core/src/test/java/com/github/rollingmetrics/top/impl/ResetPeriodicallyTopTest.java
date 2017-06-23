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

package com.github.rollingmetrics.top.impl;

import com.github.rollingmetrics.top.Top;
import com.github.rollingmetrics.util.Clock;
import com.github.rollingmetrics.util.MockExecutor;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static com.github.rollingmetrics.top.TopTestData.*;
import static com.github.rollingmetrics.top.impl.TopTestUtil.assertEmpty;
import static com.github.rollingmetrics.top.impl.TopTestUtil.checkOrder;
import static com.github.rollingmetrics.top.impl.TopTestUtil.update;


public class ResetPeriodicallyTopTest {

    @Test
    public void testCommonAspects() {
        for (int i = 1; i <= 2; i++) {
            Top top = Top.builder(i)
                    .resetAllPositionsPeriodically(Duration.ofDays(1))
                    .withSnapshotCachingDuration(Duration.ZERO)
                    .withLatencyThreshold(Duration.ofMillis(100))
                    .withMaxLengthOfQueryDescription(1000)
                    .build();
            TopTestUtil.testCommonScenarios(i, top, Duration.ofMillis(100).toNanos(), 1000);
        }
    }


    @Test
    public void test_size_1() throws Exception {
        AtomicLong currentTimeMillis = new AtomicLong(0L);
        Clock clock = Clock.mock(currentTimeMillis);
        Top top = Top.builder(1)
                .resetAllPositionsPeriodically(Duration.ofSeconds(1))
                .withSnapshotCachingDuration(Duration.ZERO)
                .withClock(clock)
                .withBackgroundExecutor(MockExecutor.INSTANCE)
                .build();

        assertEmpty(top);

        update(top, first);
        checkOrder(top, first);

        currentTimeMillis.addAndGet(500L); //500
        checkOrder(top, first);

        currentTimeMillis.addAndGet(500L); //1000
        assertEmpty(top);

        update(top, second);
        checkOrder(top, second);
        checkOrder(top, second);

        currentTimeMillis.addAndGet(1L); //1001
        update(top, first);
        checkOrder(top, second);

        currentTimeMillis.addAndGet(1000L); //2001
        assertEmpty(top);


        update(top, first);
        update(top, second);
        update(top, third);
        checkOrder(top, third);

        currentTimeMillis.addAndGet(999L); //3000
        assertEmpty(top);

        currentTimeMillis.addAndGet(1000L); //4000
        assertEmpty(top);
    }

    @Test
    public void test_size_3() throws Exception {
        AtomicLong currentTimeMillis = new AtomicLong(0L);
        Clock clock = Clock.mock(currentTimeMillis);
        Top top = Top.builder(3)
                .resetAllPositionsPeriodically(Duration.ofSeconds(1))
                .withSnapshotCachingDuration(Duration.ZERO)
                .withClock(clock)
                .withBackgroundExecutor(MockExecutor.INSTANCE)
                .build();

        assertEmpty(top);

        update(top, first);
        update(top, second);
        checkOrder(top, second, first);

        currentTimeMillis.addAndGet(500L); //500
        checkOrder(top, second, first);

        currentTimeMillis.addAndGet(500L); //1000
        assertEmpty(top);

        update(top, second);
        checkOrder(top, second);
        checkOrder(top, second);

        currentTimeMillis.addAndGet(1L); //1001
        update(top, first);
        update(top, third);
        checkOrder(top, third, second, first);

        currentTimeMillis.addAndGet(1000L); //2001
        assertEmpty(top);


        update(top, fourth);
        update(top, first);
        update(top, second);
        update(top, third);
        checkOrder(top, fourth, third, second);

        currentTimeMillis.addAndGet(999L); //3000
        assertEmpty(top);

        currentTimeMillis.addAndGet(1000L); //4000
        assertEmpty(top);
    }

    @Test
    public void testToString() {
        for (int i = 1; i <= 2; i++) {
            System.out.println(Top.builder(i)
                    .resetAllPositionsPeriodically(Duration.ofDays(1))
                    .build());
        }
    }

    @Test(timeout = 32000)
    public void testThatConcurrentThreadsNotHung_1() throws InterruptedException {
        Top top = Top.builder(1)
                .resetAllPositionsPeriodically(Duration.ofSeconds(1))
                .withSnapshotCachingDuration(Duration.ZERO)
                .build();
        TopTestUtil.runInParallel(top, TimeUnit.SECONDS.toMillis(30), 0, 10_000);
    }

    @Test(timeout = 35000)
    public void testThatConcurrentThreadsNotHung_3() throws InterruptedException {
        Top top = Top.builder(3)
                .resetAllPositionsPeriodically(Duration.ofSeconds(1))
                .withSnapshotCachingDuration(Duration.ZERO)
                .build();
        TopTestUtil.runInParallel(top, TimeUnit.SECONDS.toMillis(30), 0, 10_000);
    }

}