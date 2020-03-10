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
import com.github.rollingmetrics.top.TopTestData;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;


public class UniformRankingTest {

    @Test
    public void testCommonAspects() {
        for (int i = 1; i <= 2; i++) {
            Ranking ranking = Ranking.builder(i)
                    .neverResetPositions()
                    .withSnapshotCachingDuration(Duration.ZERO)
                    .withLatencyThreshold(Duration.ofMillis(100))
                    .withMaxLengthOfQueryDescription(1000)
                    .build();
            RankingTestUtil.testCommonScenarios(i, ranking, Duration.ofMillis(100).toNanos(), 1000);
        }
    }

    @Test
    public void test_size_1() throws Exception {
        Ranking ranking = Ranking.builder(1)
                .neverResetPositions()
                .withSnapshotCachingDuration(Duration.ZERO)
                .build();

        RankingTestUtil.assertEmpty(ranking);

        RankingTestUtil.update(ranking, TopTestData.first);
        RankingTestUtil.checkOrder(ranking, TopTestData.first);

        RankingTestUtil.update(ranking, TopTestData.second);
        RankingTestUtil.checkOrder(ranking, TopTestData.second);

        RankingTestUtil.update(ranking, TopTestData.first);
        RankingTestUtil.checkOrder(ranking, TopTestData.second);
    }

    @Test
    public void test_size_3() throws Exception {
        Ranking ranking = Ranking.builder(3)
                .neverResetPositions()
                .withSnapshotCachingDuration(Duration.ZERO)
                .build();

        RankingTestUtil.assertEmpty(ranking);

        RankingTestUtil.update(ranking, TopTestData.first);
        RankingTestUtil.checkOrder(ranking, TopTestData.first);

        RankingTestUtil.update(ranking, TopTestData.second);
        RankingTestUtil.checkOrder(ranking, TopTestData.second, TopTestData.first);

        RankingTestUtil.update(ranking, TopTestData.third);
        RankingTestUtil.checkOrder(ranking, TopTestData.third, TopTestData.second, TopTestData.first);

        RankingTestUtil.update(ranking, TopTestData.fourth);
        RankingTestUtil.checkOrder(ranking, TopTestData.fourth, TopTestData.third, TopTestData.second);

        RankingTestUtil.update(ranking, TopTestData.fifth);
        RankingTestUtil.checkOrder(ranking, TopTestData.fifth, TopTestData.fourth, TopTestData.third);

        RankingTestUtil.update(ranking, TopTestData.first);
        RankingTestUtil.checkOrder(ranking, TopTestData.fifth, TopTestData.fourth, TopTestData.third);

        RankingTestUtil.update(ranking, TopTestData.fifth);
        RankingTestUtil.checkOrder(ranking, TopTestData.fifth, TopTestData.fourth, TopTestData.third);
    }

    @Test
    public void testToString() {
        for (int i = 1; i <= 2; i++) {
            System.out.println(Ranking.builder(i)
                    .neverResetPositions()
                    .build());
        }
    }

    @Test(timeout = 32000)
    public void testThatConcurrentThreadsNotHung() throws InterruptedException {
        Ranking ranking = Ranking.builder(1)
                .neverResetPositions()
                .withSnapshotCachingDuration(Duration.ZERO)
                .build();
        RankingTestUtil.runInParallel(ranking, TimeUnit.SECONDS.toMillis(30), 0, 10_000);
    }

}