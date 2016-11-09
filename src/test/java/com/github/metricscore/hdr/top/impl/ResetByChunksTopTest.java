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

package com.github.metricscore.hdr.top.impl;

import com.github.metricscore.hdr.top.Top;
import com.github.metricscore.hdr.util.Clock;
import com.github.metricscore.hdr.util.MockExecutor;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static com.github.metricscore.hdr.top.TestData.*;
import static com.github.metricscore.hdr.top.impl.TopTestUtil.*;


public class ResetByChunksTopTest {

    @Test
    public void testCommonAspects() {
        for (int i = 1; i <= 2; i++) {
            Top top = Top.builder(i)
                    .resetAllPositionsPeriodicallyByChunks(Duration.ofDays(1), 3)
                    .withSnapshotCachingDuration(Duration.ZERO)
                    .withLatencyThreshold(Duration.ofMillis(100))
                    .withMaxLengthOfQueryDescription(1000)
                    .build();
            testCommonScenarios(i, top, Duration.ofMillis(100).toNanos(), 1000);
        }
    }

    @Test
    public void test_size_1() throws Exception {
        AtomicLong currentTimeMillis = new AtomicLong(0L);
        Clock clock = Clock.mock(currentTimeMillis);
        Top top = Top.builder(1)
                .resetAllPositionsPeriodicallyByChunks(Duration.ofSeconds(3), 3)
                .withSnapshotCachingDuration(Duration.ZERO)
                .withClock(clock)
                .withBackgroundExecutor(MockExecutor.INSTANCE)
                .build();

        assertEmpty(top);

        update(top, fifth);
        checkOrder(top, fifth);

        currentTimeMillis.addAndGet(500L); //500
        checkOrder(top, fifth);

        currentTimeMillis.addAndGet(500L); //1000
        checkOrder(top, fifth);

        update(top, fourth);
        checkOrder(top, fifth);
        checkOrder(top, fifth);

        currentTimeMillis.addAndGet(1L); //1001
        update(top, first);
        checkOrder(top, fifth);

        currentTimeMillis.addAndGet(1000L); //2001
        checkOrder(top, fifth);

        update(top, first);
        update(top, second);
        update(top, third);
        checkOrder(top, fifth);

        currentTimeMillis.addAndGet(999L); //3000
        checkOrder(top, fifth);

        currentTimeMillis.addAndGet(1L); //3001
        update(top, second);
        checkOrder(top, fifth);

        currentTimeMillis.addAndGet(999L); //4000
        update(top, first);
        checkOrder(top, fourth);

        currentTimeMillis.addAndGet(1000L); //5000
        checkOrder(top, third);

        currentTimeMillis.addAndGet(1000L); //6000
        checkOrder(top, second);

        currentTimeMillis.addAndGet(1000L); //7000
        checkOrder(top, first);

        currentTimeMillis.addAndGet(1000L); //8000
        assertEmpty(top);

        currentTimeMillis.addAndGet(2999L); //10_999
        assertEmpty(top);

        update(top, second);
        checkOrder(top, second);

        currentTimeMillis.addAndGet(3000L); //13_999
        checkOrder(top, second);

        currentTimeMillis.addAndGet(1L); //14_000
        assertEmpty(top);
    }

    @Test
    public void test_size_3() throws Exception {
        AtomicLong currentTimeMillis = new AtomicLong(0L);
        Clock clock = Clock.mock(currentTimeMillis);
        Top top = Top.builder(3)
                .resetAllPositionsPeriodicallyByChunks(Duration.ofSeconds(3), 3)
                .withSnapshotCachingDuration(Duration.ZERO)
                .withClock(clock)
                .withBackgroundExecutor(MockExecutor.INSTANCE)
                .build();

        assertEmpty(top);

        update(top, fifth);
        checkOrder(top, fifth);

        currentTimeMillis.addAndGet(500L); //500
        checkOrder(top, fifth);

        currentTimeMillis.addAndGet(500L); //1000
        checkOrder(top, fifth);

        update(top, fourth);
        checkOrder(top, fifth, fourth);
        checkOrder(top, fifth, fourth);

        currentTimeMillis.addAndGet(1L); //1001
        update(top, first);
        checkOrder(top, fifth, fourth, first);

        currentTimeMillis.addAndGet(1000L); //2001
        checkOrder(top, fifth, fourth, first);

        update(top, first);
        update(top, second);
        update(top, third);
        checkOrder(top, fifth, fourth, third);

        currentTimeMillis.addAndGet(999L); //3000
        checkOrder(top, fifth, fourth, third);

        currentTimeMillis.addAndGet(1L); //3001
        update(top, second);
        checkOrder(top, fifth, fourth, third);

        currentTimeMillis.addAndGet(999L); //4000
        update(top, first);
        checkOrder(top, fourth, third, second);

        currentTimeMillis.addAndGet(1000L); //5000
        checkOrder(top, third, second, first);

        currentTimeMillis.addAndGet(1000L); //6000
        checkOrder(top, second, first);

        currentTimeMillis.addAndGet(1000L); //7000
        checkOrder(top, first);

        currentTimeMillis.addAndGet(1000L); //8000
        assertEmpty(top);

        currentTimeMillis.addAndGet(2999L); //10_999
        assertEmpty(top);

        update(top, second);
        checkOrder(top, second);

        currentTimeMillis.addAndGet(3000L); //13_999
        checkOrder(top, second);

        currentTimeMillis.addAndGet(1L); //14_000
        assertEmpty(top);
    }

    @Test
    public void testToString() {
        for (int i = 1; i <= 2; i++) {
            System.out.println(Top.builder(i)
                    .resetAllPositionsPeriodicallyByChunks(Duration.ofDays(1), 3)
                    .build());
        }
    }

    @Test(timeout = 32000)
    public void testThatConcurrentThreadsNotHung_1() throws InterruptedException {
        Top top = Top.builder(1)
                .resetAllPositionsPeriodicallyByChunks(Duration.ofSeconds(2), 2)
                .withSnapshotCachingDuration(Duration.ZERO)
                .build();
        TopTestUtil.runInParallel(top, Duration.ofSeconds(30), 0, 10_000);
    }

    @Test(timeout = 32000)
    public void testThatConcurrentThreadsNotHung_3() throws InterruptedException {
        Top top = Top.builder(3)
                .resetAllPositionsPeriodicallyByChunks(Duration.ofSeconds(2), 2)
                .withSnapshotCachingDuration(Duration.ZERO)
                .build();
        TopTestUtil.runInParallel(top, Duration.ofSeconds(30), 0, 10_000);
    }

}