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

package com.github.metricscore.hdr.top.basic;
import org.HdrHistogram.WriterReaderPhaser;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Is not a part of public API, this class just used as building block for different QueryTop implementations.
 */
public class TopRecorder {

    private final WriterReaderPhaser recordingPhaser = new WriterReaderPhaser();

    private volatile ComposableTop active;
    private ComposableTop inactive;

    public TopRecorder(int size, long slowQueryThresholdNanos, int maxLengthOfQueryDescription) {
        this.active = ComposableTop.createConcurrentTop(size, slowQueryThresholdNanos, maxLengthOfQueryDescription);
        this.inactive = null;
    }

    public boolean update(long latencyTime, TimeUnit latencyUnit, Supplier<String> descriptionSupplier) {
        long criticalValueAtEnter = recordingPhaser.writerCriticalSectionEnter();
        try {
            return active.update(latencyTime, latencyUnit, descriptionSupplier);
        } finally {
            recordingPhaser.writerCriticalSectionExit(criticalValueAtEnter);
        }
    }

    public synchronized void reset() {
        // the currently inactive top is reset each time we flip. So flipping twice resets both:
        performIntervalSample();
        performIntervalSample();
    }

    public synchronized ComposableTop getIntervalQueryTop() {
        return getIntervalQueryTop(null);
    }

    public synchronized ComposableTop getIntervalQueryTop(ComposableTop queryTopToRecycle) {
        inactive = queryTopToRecycle;
        performIntervalSample();
        ComposableTop sampledQueryTop = inactive;
        inactive = null; // Once we expose the sample, we can't reuse it internally until it is recycled
        return sampledQueryTop;
    }

    private void performIntervalSample() {
        try {
            recordingPhaser.readerLock();

            // Make sure we have an inactive version to flip in:
            if (inactive == null) {
                inactive = ComposableTop.createNonConcurrentEmptyCopy(active);
            } else {
                inactive.reset();
            }

            // Swap active and inactive top:
            final ComposableTop temp = inactive;
            inactive = active;
            active = temp;

            recordingPhaser.flipPhase();
        } finally {
            recordingPhaser.readerUnlock();
        }
    }

}