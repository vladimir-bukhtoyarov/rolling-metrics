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

import org.HdrHistogram.WriterReaderPhaser;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Is not a part of public API, this class just used as building block for different QueryTop implementations.
 */
class QueryTopRecorder {

    private final WriterReaderPhaser recordingPhaser = new WriterReaderPhaser();

    private volatile ConcurrentQueryTop active;
    private ConcurrentQueryTop inactive;

    QueryTopRecorder(int size, Duration slowQueryThreshold) {
        active = new ConcurrentQueryTop(size, slowQueryThreshold);
        inactive = new ConcurrentQueryTop(size, slowQueryThreshold);
    }

    public void update(long latencyTime, TimeUnit latencyUnit, Supplier<String> descriptionSupplier) {
        long criticalValueAtEnter = recordingPhaser.writerCriticalSectionEnter();
        try {
            active.update(latencyTime, latencyUnit, descriptionSupplier);
        } finally {
            recordingPhaser.writerCriticalSectionExit(criticalValueAtEnter);
        }
    }

    public synchronized ConcurrentQueryTop getIntervalTop() {
        return getIntervalTop(null);
    }

    public synchronized ConcurrentQueryTop getIntervalTop(ConcurrentQueryTop queryTopToRecycle) {
        inactive = queryTopToRecycle;
        performIntervalSample();
        ConcurrentQueryTop sampledQueryTop = inactive;
        inactive = null; // Once we expose the sample, we can't reuse it internally until it is recycled
        return sampledQueryTop;
    }

    /**
     * Reset any value counts accumulated thus far.
     */
    public synchronized void reset() {
        // the currently inactive query-top is reset each time we flip. So flipping twice resets both:
        performIntervalSample();
        performIntervalSample();
    }

    private void performIntervalSample() {
        try {
            recordingPhaser.readerLock();

            // Make sure we have an inactive version to flip in:
            if (inactive == null) {
                inactive = new ConcurrentQueryTop(active.getSize(), Duration.ofNanos(active.getSlowQueryThresholdNanos()));
            } else {
                inactive.reset();
            }

            // Swap active and inactive top:
            final ConcurrentQueryTop temp = inactive;
            inactive = active;
            active = temp;

            recordingPhaser.flipPhase();
        } finally {
            recordingPhaser.readerUnlock();
        }
    }

}