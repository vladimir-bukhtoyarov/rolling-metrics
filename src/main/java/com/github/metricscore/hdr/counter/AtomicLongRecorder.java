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

package com.github.metricscore.hdr.counter;

import org.HdrHistogram.WriterReaderPhaser;

import java.util.concurrent.atomic.AtomicLong;

class AtomicLongRecorder {

    private final WriterReaderPhaser recordingPhaser = new WriterReaderPhaser();

    private volatile AtomicLong activeAtomic;
    private AtomicLong inactiveAtomic;

    public AtomicLongRecorder() {
        activeAtomic = new AtomicLong();
        inactiveAtomic = new AtomicLong();
    }

    public void add(final long value) {
        long criticalValueAtEnter = recordingPhaser.writerCriticalSectionEnter();
        try {
            activeAtomic.addAndGet(value);
        } finally {
            recordingPhaser.writerCriticalSectionExit(criticalValueAtEnter);
        }
    }

    public synchronized AtomicLong getIntervalAtomic() {
        return getIntervalAtomic(null);
    }

    public synchronized AtomicLong getIntervalAtomic(AtomicLong atomicToRecycle) {
        inactiveAtomic = atomicToRecycle;
        performIntervalSample();
        AtomicLong sampledAtomic = inactiveAtomic;
        inactiveAtomic = null; // Once we expose the sample, we can't reuse it internally until it is recycled
        return sampledAtomic;
    }

    /**
     * Reset any value counts accumulated thus far.
     */
    public synchronized void reset() {
        // the currently inactive histogram is reset each time we flip. So flipping twice resets both:
        performIntervalSample();
        performIntervalSample();
    }

    private void performIntervalSample() {
        try {
            recordingPhaser.readerLock();

            // Make sure we have an inactive version to flip in:
            if (inactiveAtomic == null) {
                inactiveAtomic = new AtomicLong();
            } else {
                inactiveAtomic.set(0);
            }

            // Swap active and inactive histograms:
            final AtomicLong temp = inactiveAtomic;
            inactiveAtomic = activeAtomic;
            activeAtomic = temp;

            // Make sure we are not in the middle of recording a value on the previously active histogram:
            // Flip phase to make sure no recordings that were in flight pre-flip are still active:
            recordingPhaser.flipPhase(500000L /* yield in 0.5 msec units if needed */);
        } finally {
            recordingPhaser.readerUnlock();
        }
    }


}
