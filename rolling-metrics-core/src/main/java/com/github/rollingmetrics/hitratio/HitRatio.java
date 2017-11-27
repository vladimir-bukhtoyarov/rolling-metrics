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

import com.github.rollingmetrics.hitratio.impl.UniformHitRatio;

/**
 * The metric for hit ratio measurement.
 *
 *
 * TODO
 *
 * @see SmoothlyDecayingRollingHitRatio
 * @see ResetOnSnapshotHitRatio
 * @see ResetPeriodicallyHitRatio
 * @see UniformHitRatio
 */
public interface HitRatio {

    /**
     * Registers the fact of single hit.
     *
     * <p> Example of usage:
     * <pre><code>
     *         Something cached = cache.get(id);
     *         if (cached != null) {
     *             hitRatio.incrementHitCount();
     *         } else {
     *             hitRatio.incrementMissCount();
     *         }
     *     </code>
     * </pre>
     */
    default void incrementHitCount() {
        update(1, 1);
    }

    /**
     * Registers the fact of single miss.
     *
     * <p> Example of usage:
     * <pre><code>
     *         Something cached = cache.get(id);
     *         if (cached != null) {
     *             hitRatio.incrementHitCount();
     *         } else {
     *             hitRatio.incrementMissCount();
     *         }
     *     </code>
     * </pre>
     */
    default void incrementMissCount() {
        update(0, 1);
    }

    /**
     * Registers an result of bulk operations.
     *
     * <p> Example of usage:
     * <pre>
     * {@code
     * Set<Something> cachedValues = cache.get(keys);
     * hitRatio.update(cachedValues.size(), keys.size());
     * }
     * </pre>
     *
     * @param hitCount
     * @param totalCount
     *
     * @throws IllegalArgumentException In case of:<ul>
     *     <li>{@code hitCount < 0}</li>
     *     <li>{@code totalCount < 1}</li>
     *     <li>{@code hitCount > totalCount}</li>
     * </ul>
     */
    void update(int hitCount, int totalCount);

    /**
     * Returns the ratio between hits and misses
     *
     * @return the ratio between hits and misses
     */
    double getHitRatio();

}
