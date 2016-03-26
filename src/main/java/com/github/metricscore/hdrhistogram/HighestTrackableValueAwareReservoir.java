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

package com.github.metricscore.hdrhistogram;

import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Snapshot;

/**
 * Decorator for {@link HdrReservoir} which knows what need to do when recording value is exceeds highestTrackableValue.
 *
 * This class is not the part of metrics-core-hdr public API and should not be used by user directly.
 */
class HighestTrackableValueAwareReservoir implements Reservoir {

    private final Reservoir target;
    private final long highestTrackableValue;
    private final OverflowResolver resolver;

    public HighestTrackableValueAwareReservoir(Reservoir target, long highestTrackableValue, OverflowResolver resolver) {
        this.target = target;
        this.highestTrackableValue = highestTrackableValue;
        this.resolver = resolver;
    }

    @Override
    public int size() {
        return target.size();
    }

    @Override
    public void update(long value) {
        if (value <= highestTrackableValue) {
            target.update(value);
            return;
        }
        switch (resolver) {
            case SKIP: break;
            case PASS_THRU: target.update(value); break;
            case REDUCE_TO_HIGHEST_TRACKABLE: target.update(highestTrackableValue); break;
        }
    }

    @Override
    public Snapshot getSnapshot() {
        return target.getSnapshot();
    }

}
