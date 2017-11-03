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

import java.util.concurrent.atomic.AtomicLong;

/**
 * The hit-ratio which reset its state to zero after each invocation of {@link #getHitRatio()}.
 *
 * <p>
 * Concurrency properties:
 * <ul>
 *     <li>Writing is lock-free. Writers do not block writers and readers.</li>
 *     <li>Reading is lock-free. Readers do not block writers and readers.</li>
 * </ul>
 *
 * <p>
 * Usage recommendations:
 * <ul>
 *     <li>When you do not need in "rolling time window" semantic. Else use {@link SmoothlyDecayingRollingHitRatio}</li>
 *     <li>When you need in 100 percents guarantee that one measure can not be reported twice.</li>
 *     <li>Only if one kind of reader interests in value of hit-ratio.
 *     Usage of this implementation for case of multiple readers will be a bad idea because of readers will steal data from each other.
 *     </li>
 * </ul>
 *
 * @see SmoothlyDecayingRollingHitRatio
 * @see ResetPeriodicallyHitRatio
 * @see UniformHitRatio
 */
class ResetOnSnapshotHitRatio implements HitRatio {

    private final AtomicLong ratio = new AtomicLong();

    @Override
    public void update(int hitCount, int totalCount) {
        HitRatioUtil.updateRatio(ratio, hitCount, totalCount);
    }

    @Override
    public double getHitRatio() {
        while (true) {
            long currentValue = ratio.get();
            if (ratio.compareAndSet(currentValue, 0L)) {
                return HitRatioUtil.getRatio(currentValue);
            }
        }
    }

}
