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

package com.github.rollingmetrics.ranking.impl;

import com.github.rollingmetrics.ranking.Ranking;
import com.github.rollingmetrics.ranking.impl.util.RankingTestUtil;
import com.github.rollingmetrics.util.Ticker;
import com.github.rollingmetrics.util.MockExecutor;
import com.github.rollingmetrics.ranking.impl.util.RankingTestData;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;


public class ResetByChunksRankingTest {

    @Test
    public void testCommonAspects() {
        for (int i = 1; i <= 2; i++) {
            Ranking ranking = Ranking.builder(i)
                    .resetPositionsPeriodicallyByChunks(Duration.ofDays(1), 3)
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
                .resetPositionsPeriodicallyByChunks(Duration.ofSeconds(3), 3)
                .withSnapshotCachingDuration(Duration.ZERO)
                .withTicker(ticker)
                .withBackgroundExecutor(MockExecutor.INSTANCE)
                .build();

        RankingTestUtil.assertEmpty(ranking);

        RankingTestUtil.update(ranking, RankingTestData.fifth);
        RankingTestUtil.checkOrder(ranking, RankingTestData.fifth);

        currentTimeMillis.addAndGet(500L); //500
        RankingTestUtil.checkOrder(ranking, RankingTestData.fifth);

        currentTimeMillis.addAndGet(500L); //1000
        RankingTestUtil.checkOrder(ranking, RankingTestData.fifth);

        RankingTestUtil.update(ranking, RankingTestData.fourth);
        RankingTestUtil.checkOrder(ranking, RankingTestData.fifth);
        RankingTestUtil.checkOrder(ranking, RankingTestData.fifth);

        currentTimeMillis.addAndGet(1L); //1001
        RankingTestUtil.update(ranking, RankingTestData.first);
        RankingTestUtil.checkOrder(ranking, RankingTestData.fifth);

        currentTimeMillis.addAndGet(1000L); //2001
        RankingTestUtil.checkOrder(ranking, RankingTestData.fifth);

        RankingTestUtil.update(ranking, RankingTestData.first);
        RankingTestUtil.update(ranking, RankingTestData.second);
        RankingTestUtil.update(ranking, RankingTestData.third);
        RankingTestUtil.checkOrder(ranking, RankingTestData.fifth);

        currentTimeMillis.addAndGet(999L); //3000
        RankingTestUtil.checkOrder(ranking, RankingTestData.fifth);

        currentTimeMillis.addAndGet(1L); //3001
        RankingTestUtil.update(ranking, RankingTestData.second);
        RankingTestUtil.checkOrder(ranking, RankingTestData.fifth);

        currentTimeMillis.addAndGet(999L); //4000
        RankingTestUtil.update(ranking, RankingTestData.first);
        RankingTestUtil.checkOrder(ranking, RankingTestData.fourth);

        currentTimeMillis.addAndGet(1000L); //5000
        RankingTestUtil.checkOrder(ranking, RankingTestData.second);

        currentTimeMillis.addAndGet(1000L); //6000
        RankingTestUtil.checkOrder(ranking, RankingTestData.second);

        currentTimeMillis.addAndGet(1000L); //7000
        RankingTestUtil.checkOrder(ranking, RankingTestData.first);

        currentTimeMillis.addAndGet(1000L); //8000
        RankingTestUtil.assertEmpty(ranking);

        currentTimeMillis.addAndGet(2999L); //10_999
        RankingTestUtil.assertEmpty(ranking);

        RankingTestUtil.update(ranking, RankingTestData.second);
        RankingTestUtil.checkOrder(ranking, RankingTestData.second);

        currentTimeMillis.addAndGet(3000L); //13_999
        RankingTestUtil.checkOrder(ranking, RankingTestData.second);

        currentTimeMillis.addAndGet(1L); //14_000
        RankingTestUtil.assertEmpty(ranking);
    }

    @Test
    public void test_size_3() throws Exception {
        AtomicLong currentTimeMillis = new AtomicLong(0L);
        Ticker ticker = Ticker.mock(currentTimeMillis);
        Ranking ranking = Ranking.builder(3)
                .resetPositionsPeriodicallyByChunks(Duration.ofSeconds(3), 3)
                .withSnapshotCachingDuration(Duration.ZERO)
                .withTicker(ticker)
                .withBackgroundExecutor(MockExecutor.INSTANCE)
                .build();

        RankingTestUtil.assertEmpty(ranking);

        RankingTestUtil.update(ranking, RankingTestData.fifth);
        RankingTestUtil.checkOrder(ranking, RankingTestData.fifth);

        currentTimeMillis.addAndGet(500L); //500
        RankingTestUtil.checkOrder(ranking, RankingTestData.fifth);

        currentTimeMillis.addAndGet(500L); //1000
        RankingTestUtil.checkOrder(ranking, RankingTestData.fifth);

        RankingTestUtil.update(ranking, RankingTestData.fourth);
        RankingTestUtil.checkOrder(ranking, RankingTestData.fifth, RankingTestData.fourth);
        RankingTestUtil.checkOrder(ranking, RankingTestData.fifth, RankingTestData.fourth);

        currentTimeMillis.addAndGet(1L); //1001
        RankingTestUtil.update(ranking, RankingTestData.first);
        RankingTestUtil.checkOrder(ranking, RankingTestData.fifth, RankingTestData.fourth, RankingTestData.first);

        currentTimeMillis.addAndGet(1000L); //2001
        RankingTestUtil.checkOrder(ranking, RankingTestData.fifth, RankingTestData.fourth, RankingTestData.first);

        RankingTestUtil.update(ranking, RankingTestData.first);
        RankingTestUtil.update(ranking, RankingTestData.second);
        RankingTestUtil.update(ranking, RankingTestData.third);
        RankingTestUtil.checkOrder(ranking, RankingTestData.fifth, RankingTestData.fourth, RankingTestData.third);

        currentTimeMillis.addAndGet(999L); //3000
        RankingTestUtil.checkOrder(ranking, RankingTestData.fifth, RankingTestData.fourth, RankingTestData.third);

//        currentTimeMillis.addAndGet(1L); //3001
//        RankingTestUtil.update(ranking, RankingTestData.second);
//        RankingTestUtil.checkOrder(ranking, RankingTestData.fifth, RankingTestData.fourth, RankingTestData.second);
//
//        currentTimeMillis.addAndGet(999L); //4000
//        RankingTestUtil.update(ranking, RankingTestData.first);
//        RankingTestUtil.checkOrder(ranking, RankingTestData.fourth, RankingTestData.second, RankingTestData.third);
//
//        currentTimeMillis.addAndGet(1000L); //5000
//        RankingTestUtil.checkOrder(ranking, RankingTestData.second, RankingTestData.third, RankingTestData.first);
//
//        currentTimeMillis.addAndGet(1000L); //6000
//        RankingTestUtil.checkOrder(ranking, RankingTestData.second, RankingTestData.first);
//
//        currentTimeMillis.addAndGet(1000L); //7000
//        RankingTestUtil.checkOrder(ranking, RankingTestData.first);
//
//        currentTimeMillis.addAndGet(1000L); //8000
//        RankingTestUtil.assertEmpty(ranking);
//
//        currentTimeMillis.addAndGet(2999L); //10_999
//        RankingTestUtil.assertEmpty(ranking);
//
//        RankingTestUtil.update(ranking, RankingTestData.second);
//        RankingTestUtil.checkOrder(ranking, RankingTestData.second);
//
//        currentTimeMillis.addAndGet(3000L); //13_999
//        RankingTestUtil.checkOrder(ranking, RankingTestData.second);
//
//        currentTimeMillis.addAndGet(1L); //14_000
//        RankingTestUtil.assertEmpty(ranking);
    }

    @Test
    public void testToString() {
        for (int i = 1; i <= 2; i++) {
            System.out.println(Ranking.builder(i)
                    .resetPositionsPeriodicallyByChunks(Duration.ofDays(1), 3)
                    .build());
        }
    }

    @Test(timeout = 32000)
    public void testThatConcurrentThreadsNotHung_1() throws InterruptedException {
        Ranking ranking = Ranking.builder(1)
                .resetPositionsPeriodicallyByChunks(Duration.ofSeconds(2), 2)
                .withSnapshotCachingDuration(Duration.ZERO)
                .build();
        RankingTestUtil.runInParallel(ranking, TimeUnit.SECONDS.toMillis(30), 0, 10_000);
    }

    @Test(timeout = 32000)
    public void testThatConcurrentThreadsNotHung_3() throws InterruptedException {
        Ranking ranking = Ranking.builder(3)
                .resetPositionsPeriodicallyByChunks(Duration.ofSeconds(2), 2)
                .withSnapshotCachingDuration(Duration.ZERO)
                .build();
        RankingTestUtil.runInParallel(ranking, TimeUnit.SECONDS.toMillis(30), 0, 10_000);
    }

}