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

package com.github.rollingmetrics.top.impl;

import com.github.rollingmetrics.top.Top;
import com.github.rollingmetrics.util.Clock;
import com.github.rollingmetrics.util.MockExecutor;
import com.github.rollingmetrics.top.TestData;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;


public class ResetByChunksTopTest {

    @Test
    public void testCommonAspects() {
        for (int i = 1; i <= 2; i++) {
            Top top = Top.builder(i)
                    .resetPositionsPeriodicallyByChunks(Duration.ofDays(1), 3)
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
                .resetPositionsPeriodicallyByChunks(Duration.ofSeconds(3), 3)
                .withSnapshotCachingDuration(Duration.ZERO)
                .withClock(clock)
                .withBackgroundExecutor(MockExecutor.INSTANCE)
                .build();

        TopTestUtil.assertEmpty(top);

        TopTestUtil.update(top, TestData.fifth);
        TopTestUtil.checkOrder(top, TestData.fifth);

        currentTimeMillis.addAndGet(500L); //500
        TopTestUtil.checkOrder(top, TestData.fifth);

        currentTimeMillis.addAndGet(500L); //1000
        TopTestUtil.checkOrder(top, TestData.fifth);

        TopTestUtil.update(top, TestData.fourth);
        TopTestUtil.checkOrder(top, TestData.fifth);
        TopTestUtil.checkOrder(top, TestData.fifth);

        currentTimeMillis.addAndGet(1L); //1001
        TopTestUtil.update(top, TestData.first);
        TopTestUtil.checkOrder(top, TestData.fifth);

        currentTimeMillis.addAndGet(1000L); //2001
        TopTestUtil.checkOrder(top, TestData.fifth);

        TopTestUtil.update(top, TestData.first);
        TopTestUtil.update(top, TestData.second);
        TopTestUtil.update(top, TestData.third);
        TopTestUtil.checkOrder(top, TestData.fifth);

        currentTimeMillis.addAndGet(999L); //3000
        TopTestUtil.checkOrder(top, TestData.fifth);

        currentTimeMillis.addAndGet(1L); //3001
        TopTestUtil.update(top, TestData.second);
        TopTestUtil.checkOrder(top, TestData.fifth);

        currentTimeMillis.addAndGet(999L); //4000
        TopTestUtil.update(top, TestData.first);
        TopTestUtil.checkOrder(top, TestData.fourth);

        currentTimeMillis.addAndGet(1000L); //5000
        TopTestUtil.checkOrder(top, TestData.third);

        currentTimeMillis.addAndGet(1000L); //6000
        TopTestUtil.checkOrder(top, TestData.second);

        currentTimeMillis.addAndGet(1000L); //7000
        TopTestUtil.checkOrder(top, TestData.first);

        currentTimeMillis.addAndGet(1000L); //8000
        TopTestUtil.assertEmpty(top);

        currentTimeMillis.addAndGet(2999L); //10_999
        TopTestUtil.assertEmpty(top);

        TopTestUtil.update(top, TestData.second);
        TopTestUtil.checkOrder(top, TestData.second);

        currentTimeMillis.addAndGet(3000L); //13_999
        TopTestUtil.checkOrder(top, TestData.second);

        currentTimeMillis.addAndGet(1L); //14_000
        TopTestUtil.assertEmpty(top);
    }

    @Test
    public void test_size_3() throws Exception {
        AtomicLong currentTimeMillis = new AtomicLong(0L);
        Clock clock = Clock.mock(currentTimeMillis);
        Top top = Top.builder(3)
                .resetPositionsPeriodicallyByChunks(Duration.ofSeconds(3), 3)
                .withSnapshotCachingDuration(Duration.ZERO)
                .withClock(clock)
                .withBackgroundExecutor(MockExecutor.INSTANCE)
                .build();

        TopTestUtil.assertEmpty(top);

        TopTestUtil.update(top, TestData.fifth);
        TopTestUtil.checkOrder(top, TestData.fifth);

        currentTimeMillis.addAndGet(500L); //500
        TopTestUtil.checkOrder(top, TestData.fifth);

        currentTimeMillis.addAndGet(500L); //1000
        TopTestUtil.checkOrder(top, TestData.fifth);

        TopTestUtil.update(top, TestData.fourth);
        TopTestUtil.checkOrder(top, TestData.fifth, TestData.fourth);
        TopTestUtil.checkOrder(top, TestData.fifth, TestData.fourth);

        currentTimeMillis.addAndGet(1L); //1001
        TopTestUtil.update(top, TestData.first);
        TopTestUtil.checkOrder(top, TestData.fifth, TestData.fourth, TestData.first);

        currentTimeMillis.addAndGet(1000L); //2001
        TopTestUtil.checkOrder(top, TestData.fifth, TestData.fourth, TestData.first);

        TopTestUtil.update(top, TestData.first);
        TopTestUtil.update(top, TestData.second);
        TopTestUtil.update(top, TestData.third);
        TopTestUtil.checkOrder(top, TestData.fifth, TestData.fourth, TestData.third);

        currentTimeMillis.addAndGet(999L); //3000
        TopTestUtil.checkOrder(top, TestData.fifth, TestData.fourth, TestData.third);

        currentTimeMillis.addAndGet(1L); //3001
        TopTestUtil.update(top, TestData.second);
        TopTestUtil.checkOrder(top, TestData.fifth, TestData.fourth, TestData.third);

        currentTimeMillis.addAndGet(999L); //4000
        TopTestUtil.update(top, TestData.first);
        TopTestUtil.checkOrder(top, TestData.fourth, TestData.third, TestData.second);

        currentTimeMillis.addAndGet(1000L); //5000
        TopTestUtil.checkOrder(top, TestData.third, TestData.second, TestData.first);

        currentTimeMillis.addAndGet(1000L); //6000
        TopTestUtil.checkOrder(top, TestData.second, TestData.first);

        currentTimeMillis.addAndGet(1000L); //7000
        TopTestUtil.checkOrder(top, TestData.first);

        currentTimeMillis.addAndGet(1000L); //8000
        TopTestUtil.assertEmpty(top);

        currentTimeMillis.addAndGet(2999L); //10_999
        TopTestUtil.assertEmpty(top);

        TopTestUtil.update(top, TestData.second);
        TopTestUtil.checkOrder(top, TestData.second);

        currentTimeMillis.addAndGet(3000L); //13_999
        TopTestUtil.checkOrder(top, TestData.second);

        currentTimeMillis.addAndGet(1L); //14_000
        TopTestUtil.assertEmpty(top);
    }

    @Test
    public void testToString() {
        for (int i = 1; i <= 2; i++) {
            System.out.println(Top.builder(i)
                    .resetPositionsPeriodicallyByChunks(Duration.ofDays(1), 3)
                    .build());
        }
    }

    @Test(timeout = 32000)
    public void testThatConcurrentThreadsNotHung_1() throws InterruptedException {
        Top top = Top.builder(1)
                .resetPositionsPeriodicallyByChunks(Duration.ofSeconds(2), 2)
                .withSnapshotCachingDuration(Duration.ZERO)
                .build();
        TopTestUtil.runInParallel(top, TimeUnit.SECONDS.toMillis(30), 0, 10_000);
    }

    @Test(timeout = 32000)
    public void testThatConcurrentThreadsNotHung_3() throws InterruptedException {
        Top top = Top.builder(3)
                .resetPositionsPeriodicallyByChunks(Duration.ofSeconds(2), 2)
                .withSnapshotCachingDuration(Duration.ZERO)
                .build();
        TopTestUtil.runInParallel(top, TimeUnit.SECONDS.toMillis(30), 0, 10_000);
    }

}