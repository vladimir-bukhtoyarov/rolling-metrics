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

package com.github.rollingmetrics.top.impl;

import com.github.rollingmetrics.retention.RetentionPolicy;
import com.github.rollingmetrics.top.Top;
import com.github.rollingmetrics.top.TopTestData;
import com.github.rollingmetrics.util.Ticker;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static com.github.rollingmetrics.top.impl.ResetByChunksTop.MAX_CHUNKS;
import static com.github.rollingmetrics.top.impl.ResetByChunksTop.MIN_CHUNK_RESETTING_INTERVAL_MILLIS;
import static junit.framework.TestCase.assertEquals;

public class TopBuilderTest {

    @Test(expected = IllegalArgumentException.class)
    public void tooManyPositionsShouldBeDisallowed() {
        RetentionPolicy.uniform()
                .newTopBuilder(TopRecorderSettings.MAX_POSITION_COUNT + 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void tooManyPositionsShouldBeDisallowed2() {
        RetentionPolicy.uniform()
                .newTopBuilder(1)
                .withPositionCount(TopRecorderSettings.MAX_POSITION_COUNT + 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void zeroPositionsShouldBeDisallowed() {
        RetentionPolicy.uniform()
                .newTopBuilder(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void zeroPositionsShouldBeDisallowed2() {
        RetentionPolicy.uniform()
                .newTopBuilder(1)
                .withPositionCount(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullSlowQueryThresholdShouldBeDisallowed() {
        RetentionPolicy.uniform()
                .newTopBuilder(1)
                .withLatencyThreshold(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeSlowQueryThresholdShouldBeDisallowed() {
        RetentionPolicy.uniform()
                .newTopBuilder(1)
                .withLatencyThreshold(Duration.ofMillis(-1));
    }

    @Test
    public void zeroSlowQueryThresholdShouldBeAllowed() {
        RetentionPolicy.uniform()
                .newTopBuilder(1)
                .withLatencyThreshold(Duration.ZERO);
    }

    @Test(expected = IllegalArgumentException.class)
    public void tooShortQueryDescriptionLengthShouldBeDisallowed() {
        RetentionPolicy.uniform()
                .newTopBuilder(1)
                .withMaxLengthOfQueryDescription(TopRecorderSettings.MIN_LENGTH_OF_QUERY_DESCRIPTION - 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void tooShortIntervalBetweenResettingShouldBeDisallowed() {
        RetentionPolicy.resetPeriodically(Duration.ofMillis(MIN_CHUNK_RESETTING_INTERVAL_MILLIS - 1))
                .newTopBuilder(1)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void tooManyChunksShouldBeDisallowed() {
        RetentionPolicy.resetPeriodicallyByChunks(Duration.ofDays(1), MAX_CHUNKS + 1)
            .newTopBuilder(1)
            .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void tooShortChunkTtlShouldBeDisallowed() {
        RetentionPolicy.resetPeriodicallyByChunks(Duration.ofMillis(MIN_CHUNK_RESETTING_INTERVAL_MILLIS * 10 - 1), 10)
            .newTopBuilder(1)
            .build();

    }

    @Test
    public void cachingPeriodShouldBeApplied() {
        AtomicLong currentTimeMillis = new AtomicLong();
        Ticker ticker = Ticker.mock(currentTimeMillis);
        Top top = RetentionPolicy.uniform()
                .withTicker(ticker)
                .withSnapshotCachingDuration(Duration.ofSeconds(10))
                .newTopBuilder(1)
                .build();

        assertEquals(1,top.getSize());

        TopTestUtil.update(top, TopTestData.first);
        TopTestUtil.checkOrder(top, TopTestData.first);

        TopTestUtil.update(top, TopTestData.second);
        TopTestUtil.checkOrder(top, TopTestData.first);

        currentTimeMillis.addAndGet(10_000);
        TopTestUtil.checkOrder(top, TopTestData.second);
    }

    @Test
    public void shouldAllowToReplaceSize() {
        Top top = RetentionPolicy.uniform()
                .newTopBuilder(1)
                .withPositionCount(2)
                .build();
        TopTestUtil.update(top, TopTestData.first);
        TopTestUtil.update(top, TopTestData.second);
        TopTestUtil.checkOrder(top, TopTestData.second, TopTestData.first);
    }

}