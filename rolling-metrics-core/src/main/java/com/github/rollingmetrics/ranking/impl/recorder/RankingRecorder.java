/*
 *    Copyright 2020 Vladimir Bukhtoyarov
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

package com.github.rollingmetrics.ranking.impl.recorder;
import org.HdrHistogram.WriterReaderPhaser;

/**
 * Is not a part of public API, this class just used as building block for different QueryTop implementations.
 */
public class RankingRecorder {

    private final WriterReaderPhaser recordingPhaser = new WriterReaderPhaser();

    private volatile ConcurrentRanking active;
    private ConcurrentRanking inactive;

    public RankingRecorder(int size, long threshold) {
        this.active = new ConcurrentRanking(size, threshold);
        this.inactive = null;
    }

    public void update(long weight, Object identity) {
        long criticalValueAtEnter = recordingPhaser.writerCriticalSectionEnter();
        try {
            active.update(weight, identity);
        } finally {
            recordingPhaser.writerCriticalSectionExit(criticalValueAtEnter);
        }
    }

    public synchronized ConcurrentRanking getIntervalRecorder() {
        return getIntervalRecorder(null);
    }

    public synchronized ConcurrentRanking getIntervalRecorder(ConcurrentRanking recorderToRecycle) {
        inactive = recorderToRecycle;
        performIntervalSample();
        ConcurrentRanking sampledQueryTop = inactive;
        inactive = null; // Once we expose the sample, we can't reuse it internally until it is recycled
        return sampledQueryTop;
    }

    private void performIntervalSample() {
        try {
            recordingPhaser.readerLock();

            // Make sure we have an inactive version to flip in:
            if (inactive == null) {
                inactive = active.createEmptyCopy();
            } else {
                inactive.resetUnsafe();
            }

            // Swap active and inactive top:
            final ConcurrentRanking temp = inactive;
            inactive = active;
            active = temp;

            recordingPhaser.flipPhase();
        } finally {
            recordingPhaser.readerUnlock();
        }
    }

}