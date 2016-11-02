/*
 *    Copyright 2016 Vladimir Bukhtoyarov
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

package com.github.metricscore.hdr.top;


import com.github.metricscore.hdr.util.Clock;
import com.github.metricscore.hdr.top.basic.BaseTop;
import com.github.metricscore.hdr.top.basic.ComposableTop;
import com.github.metricscore.hdr.top.basic.TopRecorder;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;


class ResetByChunksTop extends BaseTop {

    ResetByChunksTop(int size, long slowQueryThresholdNanos, int maxLengthOfQueryDescription, long intervalBetweenResettingMillis, int numberChunks, Clock clock, Executor backgroundExecutor) {
        super(size, slowQueryThresholdNanos, maxLengthOfQueryDescription);
        // TODO
    }

    @Override
    synchronized public List<Position> getPositionsInDescendingOrder() {
        // TODO
        return null;
    }

    @Override
    protected boolean updateImpl(long latencyTime, TimeUnit latencyUnit, Supplier<String> descriptionSupplier, long latencyNanos) {
        // TODO
        return false;
    }

}
