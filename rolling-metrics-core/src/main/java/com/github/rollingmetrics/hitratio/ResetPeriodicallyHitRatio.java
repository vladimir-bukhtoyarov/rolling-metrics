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

package com.github.rollingmetrics.hitratio;

import com.github.rollingmetrics.util.Ticker;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The hit-ratio which reset its state to zero each time when configured interval is elapsed.
 *
 * <p>
 * Concurrency properties:
 * <ul>
 *     <li>Writing is lock-free.</li>
 *     <li>Ratio calculation is lock-free.</li>
 * </ul>
 *
 * <p>
 * Usage recommendations:
 * <ul>
 *     <li>When you do not need in "rolling time window" semantic. Else use {@link SmoothlyDecayingRollingHitRatio}</li>
 *     <li>When you want to limit time which each increment takes affect to hit-ratio in order to avoid reporting of obsolete measurements.</li>
 *     <li>Only if you accept the fact that several increments can be never observed by reader(because rotation to zero can happen before reader seen the written values).</li>
 * </ul>
 *
 * @see SmoothlyDecayingRollingHitRatio
 * @see ResetPeriodicallyHitRatio
 * @see UniformHitRatio
 */
public class ResetPeriodicallyHitRatio implements HitRatio {

    private final AtomicLong ratio = new AtomicLong();
    private final long resetIntervalMillis;
    private final Ticker ticker;
    private final AtomicLong nextResetTimeMillisRef;

    /**
     * Constructs the hit-ratio which reset its state to zero each time when {@code resetInterval} is elapsed.
     *
     * @param resetInterval the interval between counter resetting
     */
    public ResetPeriodicallyHitRatio(Duration resetInterval) {
        this(resetInterval, Ticker.defaultTicker());
    }

    public ResetPeriodicallyHitRatio(Duration resetInterval, Ticker ticker) {
        if (resetInterval.isNegative() || resetInterval.isZero()) {
            throw new IllegalArgumentException("intervalBetweenChunkResetting must be a positive duration");
        }
        this.resetIntervalMillis = resetInterval.toMillis();
        this.ticker = ticker;
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
