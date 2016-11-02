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

package com.github.metricscore.hdr.top;


import com.github.metricscore.hdr.top.basic.*;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;


class UniformTop extends BaseTop {

    private final TopRecorder recorder;
    private final ComposableTop uniformQueryTop;
    private ComposableTop intervalQueryTop;

    UniformTop(int positionCount, long slowQueryThresholdNanos, int maxLengthOfQueryDescription) {
        super(positionCount, slowQueryThresholdNanos, maxLengthOfQueryDescription);
        this.recorder = new TopRecorder(positionCount, slowQueryThresholdNanos, maxLengthOfQueryDescription);
        intervalQueryTop = recorder.getIntervalQueryTop();
        this.uniformQueryTop = ComposableTop.createNonConcurrentEmptyCopy(intervalQueryTop);
    }

    @Override
    synchronized public List<Position> getPositionsInDescendingOrder() {
        intervalQueryTop = recorder.getIntervalQueryTop(intervalQueryTop);
        intervalQueryTop.addInto(uniformQueryTop);
        return uniformQueryTop.getPositionsInDescendingOrder();
    }

    @Override
    protected boolean updateImpl(long latencyTime, TimeUnit latencyUnit, Supplier<String> descriptionSupplier, long latencyNanos) {
        return recorder.update(latencyTime, latencyUnit, descriptionSupplier);
    }

}
