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

package com.github.rollingmetrics.counter;

import com.github.rollingmetrics.retention.*;
import com.github.rollingmetrics.util.Ticker;

import java.util.Objects;

/**
 * An incrementing and decrementing counter metric which having window semantic.
 *
 * @see SmoothlyDecayingRollingCounter
 * @see ResetOnSnapshotCounter
 * @see ResetPeriodicallyCounter
 */
public interface WindowCounter {

    /**
     * Increment the counter by {@code delta}.
     * If You want to decrement instead of increment then use negative {@code delta}.
     *
     * @param delta the amount by which the counter will be increased
     */
    void add(long delta);

    /**
     * Returns the counter's current value.
     *
     * @return the counter's current value
     */
    long getSum();

    /**
     * TODO
     *
     * @param retentionPolicy
     * @return
     */
    static WindowCounter build(RetentionPolicy retentionPolicy) {
        return build(retentionPolicy, Ticker.defaultTicker());
    }

    /**
     * TODO
     *
     * @param retentionPolicy
     * @param ticker
     * @return
     */
    static WindowCounter build(RetentionPolicy retentionPolicy, Ticker ticker) {
        Objects.requireNonNull(retentionPolicy);
        Objects.requireNonNull(ticker);
        if (retentionPolicy instanceof UniformRetentionPolicy) {
            return new UniformCounter();
        }
        if (retentionPolicy instanceof ResetOnSnapshotRetentionPolicy) {
            return new ResetOnSnapshotCounter();
        }
        if (retentionPolicy instanceof ResetPeriodicallyRetentionPolicy) {
            return new ResetPeriodicallyCounter((ResetPeriodicallyRetentionPolicy) retentionPolicy, ticker);
        }
        if (retentionPolicy instanceof ResetPeriodicallyByChunksRetentionPolicy) {
            return new SmoothlyDecayingRollingCounter((ResetPeriodicallyByChunksRetentionPolicy) retentionPolicy, ticker);
        }
        throw new IllegalArgumentException("Unknown retention policy " + retentionPolicy);
    }

}
