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

import java.time.Duration;

/**
 * TODO
 *
 * Reservoir configured with this strategy will be divided to <tt>numberChunks</tt> parts,
 * and one chunk will be cleared after each <tt>rollingTimeWindow / numberChunks</tt> elapsed.
 * This strategy is more smoothly then <tt>resetReservoirPeriodically</tt> because reservoir never zeroed at whole,
 * so user experience provided by <tt>resetReservoirPeriodicallyByChunks</tt> should look more pretty.
 * <p>
 * The value recorded to reservoir will take affect at least <tt>rollingTimeWindow</tt> and at most <tt>rollingTimeWindow *(1 + 1/numberChunks)</tt> time,
 * for example when you configure <tt>rollingTimeWindow=60 seconds and numberChunks=6</tt> then each value recorded to reservoir will be stored at <tt>60-70 seconds</tt>
 * </p>
 *
 * <p>
 *     If You use this strategy inside JEE environment,
 *     then it would be better to call {@code ResilientExecutionUtil.getInstance().shutdownBackgroundExecutor()}
 *     once in application shutdown listener,
 *     in order to avoid leaking reference to classloader through the thread which this library creates for histogram rotation in background.
 * </p>
 *
 */
public class ResetPeriodicallyByChunksRetentionPolicy implements RetentionPolicy {

    private final int numberChunks;
    private final long intervalBetweenResettingOneChunkMillis;
    private final Duration rollingTimeWindow;

    /**
     * TODO
     *
     * @param rollingTimeWindow the total rolling time window, any value recorded to reservoir will not be evicted from it at least <tt>rollingTimeWindow</tt>
     * @param numberChunks    specifies number of chunks by which reservoir will be slitted
     */
    public ResetPeriodicallyByChunksRetentionPolicy(int numberChunks, Duration rollingTimeWindow) {
        if (rollingTimeWindow == null) {
            throw new IllegalArgumentException("rollingTimeWindow must be a positive duration");
        }
        if (rollingTimeWindow.isNegative() || rollingTimeWindow.isZero()) {
            throw new IllegalArgumentException("resettingPeriod must be a positive duration");
        }
        if (numberChunks < 2) {
            throw new IllegalArgumentException("numberChunks must be >= 2");
        }

        this.rollingTimeWindow = rollingTimeWindow;
        this.intervalBetweenResettingOneChunkMillis = rollingTimeWindow.toMillis() / numberChunks;
        this.numberChunks = numberChunks;
    }

    public Duration getRollingTimeWindow() {
        return rollingTimeWindow;
    }

    public int getNumberChunks() {
        return numberChunks;
    }

    public long getIntervalBetweenResettingOneChunkMillis() {
        return intervalBetweenResettingOneChunkMillis;
    }

}
