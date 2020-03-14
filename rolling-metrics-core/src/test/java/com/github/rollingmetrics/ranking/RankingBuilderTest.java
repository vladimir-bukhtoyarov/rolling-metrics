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

package com.github.rollingmetrics.ranking;

import com.github.rollingmetrics.ranking.impl.util.RankingTestData;
import com.github.rollingmetrics.util.Ticker;
import com.github.rollingmetrics.ranking.impl.util.RankingTestUtil;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static com.github.rollingmetrics.ranking.RankingBuilder.MIN_CHUNK_RESETTING_INTERVAL_MILLIS;
import static junit.framework.TestCase.assertEquals;

public class RankingBuilderTest {

    @Test(expected = IllegalArgumentException.class)
    public void tooManyPositionsShouldBeDisallowed() {
        Ranking.builder(RankingBuilder.MAX_POSITION_COUNT + 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void tooManyPositionsShouldBeDisallowed2() {
        Ranking.builder(1).withPositionCount(RankingBuilder.MAX_POSITION_COUNT + 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void zeroPositionsShouldBeDisallowed() {
        Ranking.builder(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void zeroPositionsShouldBeDisallowed2() {
        Ranking.builder(1).withPositionCount(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullSlowQueryThresholdShouldBeDisallowed() {
        Ranking.builder(1).withLatencyThreshold(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeSlowQueryThresholdShouldBeDisallowed() {
        Ranking.builder(1).withLatencyThreshold(Duration.ofMillis(-1));
    }

    @Test
    public void zeroSlowQueryThresholdShouldBeAllowed() {
        Ranking.builder(1).withLatencyThreshold(Duration.ZERO);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullBackgroundExecutorShouldBeDisallowed() {
        Ranking.builder(1).withBackgroundExecutor(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullTickerShouldBeDisallowed() {
        Ranking.builder(1).withTicker(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void tooShortQueryDescriptionLengthShouldBeDisallowed() {
        Ranking.builder(1).withMaxLengthOfQueryDescription(RankingBuilder.MIN_LENGTH_OF_QUERY_DESCRIPTION - 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullIntervalBetweenResettingShouldBeDisallowed() {
        Ranking.builder(1).resetAllPositionsPeriodically(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeIntervalBetweenResettingShouldBeDisallowed() {
        Ranking.builder(1).resetAllPositionsPeriodically(Duration.ofSeconds(-60));
    }

    @Test(expected = IllegalArgumentException.class)
    public void tooShortIntervalBetweenResettingShouldBeDisallowed() {
        Ranking.builder(1).resetAllPositionsPeriodically(Duration.ofMillis(MIN_CHUNK_RESETTING_INTERVAL_MILLIS - 1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void tooManyChunksShouldBeDisallowed() {
        Ranking.builder(1).resetPositionsPeriodicallyByChunks(Duration.ofDays(1), RankingBuilder.MAX_CHUNKS + 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void lesserThanTwoChunksShouldBeDisallowed() {
        Ranking.builder(1).resetPositionsPeriodicallyByChunks(Duration.ofDays(1), 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullRollingWindowShouldBeDisallowed() {
        Ranking.builder(1).resetPositionsPeriodicallyByChunks(null, RankingBuilder.MAX_CHUNKS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void tooShortChunkTtlShouldBeDisallowed() {
        Ranking.builder(1).resetPositionsPeriodicallyByChunks(Duration.ofMillis(RankingBuilder.MIN_CHUNK_RESETTING_INTERVAL_MILLIS * 10 - 1), 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeChunkTtlShouldBeDisallowed() {
        Ranking.builder(1).resetPositionsPeriodicallyByChunks(Duration.ofMillis(-2000), 2);
    }

    @Test
    public void cachingPeriodShouldBeApplied() {
        AtomicLong currentTimeMillis = new AtomicLong();
        Ticker ticker = Ticker.mock(currentTimeMillis);
        Ranking ranking = Ranking.builder(1)
                .neverResetPositions()
                .withTicker(ticker)
                .withSnapshotCachingDuration(Duration.ofSeconds(10))
                .build();

        assertEquals(1, ranking.getSize());

        RankingTestUtil.update(ranking, RankingTestData.first);
        RankingTestUtil.checkOrder(ranking, RankingTestData.first);

        RankingTestUtil.update(ranking, RankingTestData.second);
        RankingTestUtil.checkOrder(ranking, RankingTestData.first);

        currentTimeMillis.addAndGet(10_000);
        RankingTestUtil.checkOrder(ranking, RankingTestData.second);
    }

    @Test
    public void shouldUse1SecondCachingPeriodByDefault() {
        AtomicLong currentTimeMillis = new AtomicLong();
        Ticker ticker = Ticker.mock(currentTimeMillis);
        Ranking ranking = Ranking.builder(1)
                .neverResetPositions()
                .withTicker(ticker)
                .build();

        RankingTestUtil.update(ranking, RankingTestData.first);
        RankingTestUtil.checkOrder(ranking, RankingTestData.first);

        RankingTestUtil.update(ranking, RankingTestData.second);
        RankingTestUtil.checkOrder(ranking, RankingTestData.first);

        currentTimeMillis.addAndGet(1_000);
        RankingTestUtil.checkOrder(ranking, RankingTestData.second);
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeCachingDurationShouldBeDisallowed() {
        Ranking.builder(1).withSnapshotCachingDuration(Duration.ofMillis(-2000));
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullCachingDurationShouldBeDisallowed() {
        Ranking.builder(1).withSnapshotCachingDuration(null);
    }

    @Test
    public void shouldAllowToReplaceSize() {
        Ranking ranking = Ranking.builder(1).withPositionCount(2).build();
        RankingTestUtil.update(ranking, RankingTestData.first);
        RankingTestUtil.update(ranking, RankingTestData.second);
        RankingTestUtil.checkOrder(ranking, RankingTestData.second, RankingTestData.first);
    }

}