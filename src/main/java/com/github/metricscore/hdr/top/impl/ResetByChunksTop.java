/*
 *
 *  Copyright 2016 Vladimir Bukhtoyarov
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

package com.github.metricscore.hdr.top.impl;


import com.github.metricscore.hdr.top.Position;
import com.github.metricscore.hdr.top.Top;
import com.github.metricscore.hdr.util.Clock;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;


public class ResetByChunksTop implements Top {

    public ResetByChunksTop(int size, long slowQueryThresholdNanos, int maxLengthOfQueryDescription, long intervalBetweenResettingMillis, int numberChunks, Clock clock, Executor backgroundExecutor) {
        // TODO
    }

    @Override
    public void update(long timestamp, long latencyTime, TimeUnit latencyUnit, Supplier<String> descriptionSupplier) {
        // TODO
    }

    @Override
    synchronized public List<Position> getPositionsInDescendingOrder() {
        // TODO
        return null;
    }

    @Override
    public int getSize() {
        // TODO
        return 0;
    }

}
