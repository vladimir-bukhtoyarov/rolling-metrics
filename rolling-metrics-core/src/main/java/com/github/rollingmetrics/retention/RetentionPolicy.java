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

import com.github.rollingmetrics.counter.WindowCounter;
import com.github.rollingmetrics.histogram.hdr.RollingHdrHistogram;
import com.github.rollingmetrics.histogram.hdr.RollingHdrHistogramBuilder;
import com.github.rollingmetrics.hitratio.HitRatio;
import com.github.rollingmetrics.top.Top;
import com.github.rollingmetrics.top.TopBuilder;
import com.github.rollingmetrics.util.Ticker;

import java.time.Duration;

/**
 * TODO javadocs
 */
public interface RetentionPolicy {

    static RetentionPolicy uniform() {
        return UniformRetentionPolicy.INSTANCE;
    }

    static RetentionPolicy resetOnSnapshot() {
        return ResetOnSnapshotRetentionPolicy.INSTANCE;
    }

    static RetentionPolicy resetPeriodically(Duration resettingPeriod) {
        return new ResetPeriodicallyRetentionPolicy(resettingPeriod);
    }

    static RetentionPolicy resetPeriodicallyByChunks(Duration rollingTimeWindow, int numberChunks) {
        return new ResetPeriodicallyByChunksRetentionPolicy(numberChunks, rollingTimeWindow);
    }

    default WindowCounter newCounter() {
        return WindowCounter.build(this);
    }

    default WindowCounter newCounter(Ticker ticker) {
        return WindowCounter.build(this, ticker);
    }

    default HitRatio newHitRatio() {
        return HitRatio.build(this);
    }

    default HitRatio newHitRatio(Ticker ticker) {
        return HitRatio.build(this, ticker);
    }

    default RollingHdrHistogramBuilder newRollingHdrHistogramBuilder() {
        return RollingHdrHistogram.builder(this);
    }

    /**
     * Creates new instance of {@link TopBuilder}
     *
     * @param size maximum count of positions in the top
     *
     * @return new instance of {@link TopBuilder}
     */
    default TopBuilder newTopBuilder(int size) {
        return Top.builder(size,this);
    }

}