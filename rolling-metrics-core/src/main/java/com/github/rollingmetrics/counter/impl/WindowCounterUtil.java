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

package com.github.rollingmetrics.counter.impl;

import com.github.rollingmetrics.counter.WindowCounter;
import com.github.rollingmetrics.retention.*;

import java.time.Duration;
import java.util.Objects;

/**
 * This is not part of public API.
 */
public class WindowCounterUtil {

    public static WindowCounter build(RetentionPolicy retentionPolicy) {
        WindowCounter counter = createCounter(retentionPolicy);
        return decorate(counter, retentionPolicy);
    }

    private static WindowCounter createCounter(RetentionPolicy retentionPolicy) {
        Objects.requireNonNull(retentionPolicy);
        if (retentionPolicy instanceof UniformRetentionPolicy) {
            return new UniformCounter();
        }
        if (retentionPolicy instanceof ResetOnSnapshotRetentionPolicy) {
            return new ResetOnSnapshotCounter();
        }
        if (retentionPolicy instanceof ResetPeriodicallyRetentionPolicy) {
            return new ResetPeriodicallyCounter((ResetPeriodicallyRetentionPolicy) retentionPolicy, retentionPolicy.getTicker());
        }
        if (retentionPolicy instanceof ResetPeriodicallyByChunksRetentionPolicy) {
            return new SmoothlyDecayingRollingCounter((ResetPeriodicallyByChunksRetentionPolicy) retentionPolicy, retentionPolicy.getTicker());
        }
        throw new IllegalArgumentException("Unknown retention policy " + retentionPolicy);
    }

    private static WindowCounter decorate(WindowCounter counter, RetentionPolicy retentionPolicy) {
        Duration snapshotCachingDuration = retentionPolicy.getSnapshotCachingDuration();
        if (snapshotCachingDuration.isZero()) {
            return counter;
        }
        // TODO unit test
        return new SnapshotCachingWindowCounter(retentionPolicy, counter);
    }

}
