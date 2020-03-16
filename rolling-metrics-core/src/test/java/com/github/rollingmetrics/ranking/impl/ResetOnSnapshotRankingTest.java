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
import com.github.rollingmetrics.ranking.impl.util.RankingTestData;
import com.github.rollingmetrics.ranking.impl.util.RankingTestUtil;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;


public class ResetOnSnapshotRankingTest {

    @Test
    public void testCommonAspects() {
        for (int i = 1; i <= 2; i++) {
            Ranking ranking = Ranking.builder(i)
                    .resetAllPositionsOnSnapshot()
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
                .resetAllPositionsOnSnapshot()
                .withSnapshotCachingDuration(Duration.ZERO)
                .build();

        RankingTestUtil.assertEmpty(ranking);

        RankingTestUtil.update(ranking, RankingTestData.first);
        RankingTestUtil.checkOrder(ranking, RankingTestData.first);
        RankingTestUtil.assertEmpty(ranking);

        RankingTestUtil.update(ranking, RankingTestData.second);
        RankingTestUtil.checkOrder(ranking, RankingTestData.second);
        RankingTestUtil.assertEmpty(ranking);

        RankingTestUtil.update(ranking, RankingTestData.first);
        RankingTestUtil.checkOrder(ranking, RankingTestData.first);
        RankingTestUtil.assertEmpty(ranking);

        RankingTestUtil.update(ranking, RankingTestData.first);
        RankingTestUtil.update(ranking, RankingTestData.second);
        RankingTestUtil.update(ranking, RankingTestData.third);
        RankingTestUtil.checkOrder(ranking, RankingTestData.third);
        RankingTestUtil.assertEmpty(ranking);
    }

    @Test
    public void test_size_3() throws Exception {
        Ranking ranking = Ranking.builder(3)
                .resetAllPositionsOnSnapshot()
                .withSnapshotCachingDuration(Duration.ZERO)
                .build();

        RankingTestUtil.assertEmpty(ranking);

        RankingTestUtil.update(ranking, RankingTestData.first);
        RankingTestUtil.checkOrder(ranking, RankingTestData.first);
        RankingTestUtil.assertEmpty(ranking);

        RankingTestUtil.update(ranking, RankingTestData.first);
        RankingTestUtil.update(ranking, RankingTestData.second);
        RankingTestUtil.checkOrder(ranking, RankingTestData.second, RankingTestData.first);
        RankingTestUtil.assertEmpty(ranking);

        RankingTestUtil.update(ranking, RankingTestData.third);
        RankingTestUtil.update(ranking, RankingTestData.first);
        RankingTestUtil.update(ranking, RankingTestData.second);
        RankingTestUtil.checkOrder(ranking, RankingTestData.second, RankingTestData.third, RankingTestData.first);
        RankingTestUtil.assertEmpty(ranking);
    }

    @Test
    public void testToString() {
        for (int i = 1; i <= 2; i++) {
            System.out.println(Ranking.builder(i)
                    .resetAllPositionsOnSnapshot()
                    .build());
        }
    }

    @Test(timeout = 32000)
    public void testThatConcurrentThreadsNotHung_1() throws InterruptedException {
        Ranking ranking = Ranking.builder(1)
                .resetAllPositionsOnSnapshot()
                .withSnapshotCachingDuration(Duration.ZERO)
                .build();
        RankingTestUtil.runInParallel(ranking, TimeUnit.SECONDS.toMillis(30), 0, 10_000);
    }

}