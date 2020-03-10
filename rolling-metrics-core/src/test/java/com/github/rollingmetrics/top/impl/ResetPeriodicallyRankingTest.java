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

import com.github.rollingmetrics.top.Ranking;
import com.github.rollingmetrics.util.Ticker;
import com.github.rollingmetrics.util.MockExecutor;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static com.github.rollingmetrics.top.TopTestData.*;
import static com.github.rollingmetrics.top.impl.RankingTestUtil.assertEmpty;
import static com.github.rollingmetrics.top.impl.RankingTestUtil.checkOrder;
import static com.github.rollingmetrics.top.impl.RankingTestUtil.update;


public class ResetPeriodicallyRankingTest {

    @Test
    public void testCommonAspects() {
        for (int i = 1; i <= 2; i++) {
            Ranking ranking = Ranking.builder(i)
                    .resetAllPositionsPeriodically(Duration.ofDays(1))
                    .withSnapshotCachingDuration(Duration.ZERO)
                    .withLatencyThreshold(Duration.ofMillis(100))
                    .withMaxLengthOfQueryDescription(1000)
                    .build();
            RankingTestUtil.testCommonScenarios(i, ranking, Duration.ofMillis(100).toNanos(), 1000);
        }
    }


    @Test
    public void test_size_1() throws Exception {
        AtomicLong currentTimeMillis = new AtomicLong(0L);
        Ticker ticker = Ticker.mock(currentTimeMillis);
        Ranking ranking = Ranking.builder(1)
                .resetAllPositionsPeriodically(Duration.ofSeconds(1))
                .withSnapshotCachingDuration(Duration.ZERO)
                .withTicker(ticker)
                .withBackgroundExecutor(MockExecutor.INSTANCE)
                .build();

        assertEmpty(ranking);

        update(ranking, first);
        checkOrder(ranking, first);

        currentTimeMillis.addAndGet(500L); //500
        checkOrder(ranking, first);

        currentTimeMillis.addAndGet(500L); //1000
        assertEmpty(ranking);

        update(ranking, second);
        checkOrder(ranking, second);
        checkOrder(ranking, second);

        currentTimeMillis.addAndGet(1L); //1001
        update(ranking, first);
        checkOrder(ranking, second);

        currentTimeMillis.addAndGet(1000L); //2001
        assertEmpty(ranking);


        update(ranking, first);
        update(ranking, second);
        update(ranking, third);
        checkOrder(ranking, third);

        currentTimeMillis.addAndGet(999L); //3000
        assertEmpty(ranking);

        currentTimeMillis.addAndGet(1000L); //4000
        assertEmpty(ranking);
    }

    @Test
    public void test_size_3() throws Exception {
        AtomicLong currentTimeMillis = new AtomicLong(0L);
        Ticker ticker = Ticker.mock(currentTimeMillis);
        Ranking ranking = Ranking.builder(3)
                .resetAllPositionsPeriodically(Duration.ofSeconds(1))
                .withSnapshotCachingDuration(Duration.ZERO)
                .withTicker(ticker)
                .withBackgroundExecutor(MockExecutor.INSTANCE)
                .build();

        assertEmpty(ranking);

        update(ranking, first);
        update(ranking, second);
        checkOrder(ranking, second, first);

        currentTimeMillis.addAndGet(500L); //500
        checkOrder(ranking, second, first);

        currentTimeMillis.addAndGet(500L); //1000
        assertEmpty(ranking);

        update(ranking, second);
        checkOrder(ranking, second);
        checkOrder(ranking, second);

        currentTimeMillis.addAndGet(1L); //1001
        update(ranking, first);
        update(ranking, third);
        checkOrder(ranking, third, second, first);

        currentTimeMillis.addAndGet(1000L); //2001
        assertEmpty(ranking);


        update(ranking, fourth);
        update(ranking, first);
        update(ranking, second);
        update(ranking, third);
        checkOrder(ranking, fourth, third, second);

        currentTimeMillis.addAndGet(999L); //3000
        assertEmpty(ranking);

        currentTimeMillis.addAndGet(1000L); //4000
        assertEmpty(ranking);
    }

    @Test
    public void testToString() {
        for (int i = 1; i <= 2; i++) {
            System.out.println(Ranking.builder(i)
                    .resetAllPositionsPeriodically(Duration.ofDays(1))
                    .build());
        }
    }

    @Test(timeout = 32000)
    public void testThatConcurrentThreadsNotHung_1() throws InterruptedException {
        Ranking ranking = Ranking.builder(1)
                .resetAllPositionsPeriodically(Duration.ofSeconds(1))
                .withSnapshotCachingDuration(Duration.ZERO)
                .build();
        RankingTestUtil.runInParallel(ranking, TimeUnit.SECONDS.toMillis(30), 0, 10_000);
    }

    @Test(timeout = 35000)
    public void testThatConcurrentThreadsNotHung_3() throws InterruptedException {
        Ranking ranking = Ranking.builder(3)
                .resetAllPositionsPeriodically(Duration.ofSeconds(1))
                .withSnapshotCachingDuration(Duration.ZERO)
                .build();
        RankingTestUtil.runInParallel(ranking, TimeUnit.SECONDS.toMillis(30), 0, 10_000);
    }

}