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

package com.github.metricscore.hdr.counter;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Gauge;
import com.github.metricscore.hdr.ChunkEvictionPolicy;

import java.time.Duration;

public interface WindowCounter extends Gauge<Long> {

    static WindowCounter newResetAtSnapshotCounter() {
        return new ResetAtSnapshotCounter();
    }

    static WindowCounter newResetPeriodicallyCounter(Duration duration) {
        return new ResetPeriodicallyCounter(duration.toMillis(), Clock.defaultClock());
    }

    static WindowCounter newResetByChunkCounter(ChunkEvictionPolicy evictionPolicy) {
        return new ResetByChunksCounter(evictionPolicy, Clock.defaultClock());
    }

    void add(long delta);

    long getSum();

}
