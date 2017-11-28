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
import java.util.Optional;
import java.util.concurrent.Executor;


/**
 * TODO
 *
 * Reservoir configured with this strategy will be cleared fully after each <tt>resettingPeriod</tt>.
 * <p>
 * The value recorded to reservoir will take affect at most <tt>resettingPeriod</tt> time.
 * </p>
 *
 * <p>
 *     If You use this strategy inside JEE environment,
 *     then it would be better to call {@code ResilientExecutionUtil.getInstance().shutdownBackgroundExecutor()}
 *     once in application shutdown listener,
 *     in order to avoid leaking reference to classloader through the thread which this library creates for histogram rotation in background.
 * </p>
 */
public class ResetPeriodicallyRetentionPolicy extends DefaultRetentionPolicy {

    private final Duration resettingPeriod;

    /**
     * TODO
     *
     * @param resettingPeriod specifies how often need to reset reservoir
     */
    public ResetPeriodicallyRetentionPolicy(Duration resettingPeriod) {
        if (resettingPeriod == null) {
            throw new IllegalArgumentException("resettingPeriod must not be null");
        }
        if (resettingPeriod.isNegative() || resettingPeriod.isZero()) {
            throw new IllegalArgumentException("resettingPeriod must be a positive duration");
        }
        this.resettingPeriod = resettingPeriod;
    }

    public Duration getResettingPeriod() {
        return resettingPeriod;
    }

    public long getResettingPeriodMillis() {
        return resettingPeriod.toMillis();
    }

}
