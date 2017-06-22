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
 * This is not part of public API.
 */
public class HitRatioUtil {

    static double getRatio(long compositeRatio) {
        int hit = getHitFromCompositeRatio(compositeRatio);
        int total = getTotalCountFromCompositeRatio(compositeRatio);
        return (double) hit / (double) total;
    }

    static long updateRatio(AtomicLong compositeRatioRef, int hitCount, int totalCount) {
        if (hitCount > totalCount) {
            throw new IllegalArgumentException("hitCount should be <= totalCount");
        }
        if (totalCount < 1) {
            throw new IllegalArgumentException("totalCount should be >= 1");
        }
        if (hitCount < 0) {
            throw new IllegalArgumentException("hitCount should be >= 0");
        }
        while (true) {
            long compositeRatio = compositeRatioRef.get();
            long accumulatedHit = getHitFromCompositeRatio(compositeRatio);
            accumulatedHit += hitCount;
            long accumulatedTotal = getTotalCountFromCompositeRatio(compositeRatio);
            accumulatedTotal += totalCount;

            if (accumulatedTotal > Integer.MAX_VALUE) {
                accumulatedHit /= 2;
                accumulatedTotal /= 2;
            }

            long newCompositeRatio = toLong((int) accumulatedHit, (int) accumulatedTotal);
            if (compositeRatioRef.compareAndSet(compositeRatio, newCompositeRatio)) {
                return newCompositeRatio;
            }
        }
    }

    static int getHitFromCompositeRatio(long compositeRatio) {
        return (int) (compositeRatio >> 32);
    }

    static int getTotalCountFromCompositeRatio(long compositeRatio) {
        return (int) compositeRatio;
    }

    static long toLong(int first, int second) {
        return (long) first << 32 | second & 0xFFFFFFFFL;
    }

}
