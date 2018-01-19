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

package com.github.rollingmetrics.hitratio.impl;

import com.github.rollingmetrics.hitratio.HitRatio;
import com.github.rollingmetrics.retention.ResetPeriodicallyRetentionPolicy;
import com.github.rollingmetrics.util.Ticker;

import java.util.concurrent.atomic.AtomicLong;

/**
 * The hit-ratio which reset its state to zero each time when configured interval is elapsed.
 */
class ResetPeriodicallyHitRatio implements HitRatio {

    private final AtomicLong ratio = new AtomicLong();
    private final long resetIntervalMillis;
    private final Ticker ticker;
    private final AtomicLong nextResetTimeMillisRef;

    ResetPeriodicallyHitRatio(ResetPeriodicallyRetentionPolicy retentionPolicy) {
        this.resetIntervalMillis = retentionPolicy.getResettingPeriodMillis();
        this.ticker = retentionPolicy.getTicker();
        this.nextResetTimeMillisRef = new AtomicLong(ticker.stableMilliseconds() + resetIntervalMillis);
    }

    @Override
    public void update(int hitCount, int totalCount) {
        long nextResetTimeMillis = nextResetTimeMillisRef.get();
        long currentTimeMillis = ticker.stableMilliseconds();
        if (currentTimeMillis >= nextResetTimeMillis) {
            if (nextResetTimeMillisRef.compareAndSet(nextResetTimeMillis, Long.MAX_VALUE)) {
                ratio.set(0L);
                nextResetTimeMillisRef.set(currentTimeMillis + resetIntervalMillis);
            }
        }
        HitRatioUtil.updateRatio(ratio, hitCount, totalCount);
    }

    @Override
    public double getHitRatio() {
        long nextResetTimeMillis = nextResetTimeMillisRef.get();
        long currentTimeMillis = ticker.stableMilliseconds();
        if (currentTimeMillis >= nextResetTimeMillis) {
            if (nextResetTimeMillisRef.compareAndSet(nextResetTimeMillis, Long.MAX_VALUE)) {
                ratio.set(0L);
                nextResetTimeMillisRef.set(currentTimeMillis + resetIntervalMillis);
            }
            return Double.NaN;
        } else {
            return HitRatioUtil.getRatio(ratio.get());
        }
    }

}
