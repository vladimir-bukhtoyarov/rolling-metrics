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

package com.github.metricscore.hdr.top;

import com.github.metricscore.hdr.util.Clock;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static com.github.metricscore.hdr.top.TestData.first;
import static com.github.metricscore.hdr.top.TestData.second;
import static com.github.metricscore.hdr.top.TopBuilder.MIN_CHUNK_RESETTING_INTERVAL_MILLIS;
import static com.github.metricscore.hdr.top.impl.TopTestUtil.checkOrder;
import static com.github.metricscore.hdr.top.impl.TopTestUtil.update;
import static junit.framework.TestCase.assertEquals;

public class TopBuilderTest {

    @Test(expected = IllegalArgumentException.class)
    public void tooManyPositionsShouldBeDisallowed() {
        Top.builder(TopBuilder.MAX_POSITION_COUNT + 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void tooManyPositionsShouldBeDisallowed2() {
        Top.builder(1).withPositionCount(TopBuilder.MAX_POSITION_COUNT + 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void zeroPositionsShouldBeDisallowed() {
        Top.builder(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void zeroPositionsShouldBeDisallowed2() {
        Top.builder(1).withPositionCount(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullSlowQueryThresholdShouldBeDisallowed() {
        Top.builder(1).withSlowQueryThreshold(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeSlowQueryThresholdShouldBeDisallowed() {
        Top.builder(1).withSlowQueryThreshold(Duration.ofMillis(-1));
    }

    @Test
    public void zeroSlowQueryThresholdShouldBeAllowed() {
        Top.builder(1).withSlowQueryThreshold(Duration.ZERO);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullBackgroundExecutorShouldBeDisallowed() {
        Top.builder(1).withBackgroundExecutor(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullClockShouldBeDisallowed() {
        Top.builder(1).withClock(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void tooShortQueryDescriptionLengthShouldBeDisallowed() {
        Top.builder(1).withMaxLengthOfQueryDescription(TopBuilder.MIN_LENGTH_OF_QUERY_DESCRIPTION - 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullIntervalBetweenResettingShouldBeDisallowed() {
        Top.builder(1).resetAllPositionsPeriodically(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeIntervalBetweenResettingShouldBeDisallowed() {
        Top.builder(1).resetAllPositionsPeriodically(Duration.ofSeconds(-60));
    }

    @Test(expected = IllegalArgumentException.class)
    public void tooShortIntervalBetweenResettingShouldBeDisallowed() {
        Top.builder(1).resetAllPositionsPeriodically(Duration.ofMillis(MIN_CHUNK_RESETTING_INTERVAL_MILLIS - 1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void tooManyChunksShouldBeDisallowed() {
        Top.builder(1).resetAllPositionsPeriodicallyByChunks(Duration.ofDays(1), TopBuilder.MAX_CHUNKS + 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void lesserThanTwoChunksShouldBeDisallowed() {
        Top.builder(1).resetAllPositionsPeriodicallyByChunks(Duration.ofDays(1), 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullRollingWindowShouldBeDisallowed() {
        Top.builder(1).resetAllPositionsPeriodicallyByChunks(null, TopBuilder.MAX_CHUNKS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void tooShortChunkTtlShouldBeDisallowed() {
        Top.builder(1).resetAllPositionsPeriodicallyByChunks(Duration.ofMillis(TopBuilder.MIN_CHUNK_RESETTING_INTERVAL_MILLIS * 10 - 1), 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeChunkTtlShouldBeDisallowed() {
        Top.builder(1).resetAllPositionsPeriodicallyByChunks(Duration.ofMillis(-2000), 2);
    }

    @Test
    public void cachingPeriodShouldBeApplied() {
        AtomicLong currentTimeMillis = new AtomicLong();
        Clock clock = Clock.mock(currentTimeMillis);
        Top top = Top.builder(1)
                .neverResetPositions()
                .withClock(clock)
                .withSnapshotCachingDuration(Duration.ofSeconds(10))
                .build();

        assertEquals(1,top.getSize());

        update(top, first);
        checkOrder(top, first);

        update(top, second);
        checkOrder(top, first);

        currentTimeMillis.addAndGet(10_000);
        checkOrder(top, second);
    }

    @Test
    public void shouldUse1SecondCachingPeriodByDefault() {
        AtomicLong currentTimeMillis = new AtomicLong();
        Clock clock = Clock.mock(currentTimeMillis);
        Top top = Top.builder(1)
                .neverResetPositions()
                .withClock(clock)
                .build();

        update(top, first);
        checkOrder(top, first);

        update(top, second);
        checkOrder(top, first);

        currentTimeMillis.addAndGet(1_000);
        checkOrder(top, second);
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeCachingDurationShouldBeDisallowed() {
        Top.builder(1).withSnapshotCachingDuration(Duration.ofMillis(-2000));
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullCachingDurationShouldBeDisallowed() {
        Top.builder(1).withSnapshotCachingDuration(null);
    }

    @Test
    public void shouldAllowToReplaceSize() {
        Top top = Top.builder(1).withPositionCount(2).build();
        update(top, first);
        update(top, second);
        checkOrder(top, second, first);
    }

}