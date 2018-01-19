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
import com.github.rollingmetrics.hitratio.impl.HitRatioUtil;
import com.github.rollingmetrics.hitratio.impl.ResetOnSnapshotHitRatio;
import com.github.rollingmetrics.hitratio.impl.ResetPeriodicallyHitRatio;
import com.github.rollingmetrics.hitratio.impl.SmoothlyDecayingRollingHitRatio;

import java.util.concurrent.atomic.AtomicLong;

/**
 * The hit-ratio which never evicts collected values.
 */
public class UniformHitRatio implements HitRatio {

    private final AtomicLong ratio = new AtomicLong();

    UniformHitRatio() {
        // package-private constructor to avoid initialization without builder infrastructure
    }

    @Override
    public void update(int hitCount, int totalCount) {
        HitRatioUtil.updateRatio(ratio, hitCount, totalCount);
    }

    @Override
    public double getHitRatio() {
        return HitRatioUtil.getRatio(ratio.get());
    }

}
