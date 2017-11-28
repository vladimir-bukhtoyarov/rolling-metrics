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

package com.github.rollingmetrics.retention;

import com.github.rollingmetrics.histogram.hdr.RollingHdrHistogram;
import com.github.rollingmetrics.top.Top;
import com.github.rollingmetrics.top.TopBuilder;
import org.junit.Test;

import java.time.Duration;

public class ResetPeriodicallyByChunksRetentionPolicyTest {

    @Test(expected = IllegalArgumentException.class)
    public void shouldDisAllowLessThenOneChunks() {
        new SmoothlyDecayingRollingCounter(Duration.ofSeconds(1), 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullRollingWindowShouldBeDisallowed() {
        Top.builder(1).resetPositionsPeriodicallyByChunks(null, TopBuilder.MAX_CHUNKS);
    }

    @Test
    public void shouldAllowOneChunk() {
        new SmoothlyDecayingRollingCounter(Duration.ofSeconds(1), 0);
    }

    @Test
    public void shouldAllowTwoChunks() {
        new SmoothlyDecayingRollingCounter(Duration.ofSeconds(1), 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldDisallowNegativeDuration() {
        RollingHdrHistogram.builder()
                .resetReservoirPeriodicallyByChunks(Duration.ofMillis(-1), 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeChunkTtlShouldBeDisallowed() {
        Top.builder(1).resetPositionsPeriodicallyByChunks(Duration.ofMillis(-2000), 2);
    }

}