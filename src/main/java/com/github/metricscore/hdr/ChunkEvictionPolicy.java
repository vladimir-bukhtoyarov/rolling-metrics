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

package com.github.metricscore.hdr;

import java.time.Duration;

/**
 * Created by vladimir.bukhtoyarov on 02.09.2016.
 */
public class ChunkEvictionPolicy {

    private final long resettingPeriodMillis;
    private final int numberChunks;
    private final boolean reportUncompletedChunkToSnapshot;
    private final boolean smoothlyEvictFromOldestChunk;

    public ChunkEvictionPolicy(Duration resettingPeriod, int numberChunks) {
        this(resettingPeriod, numberChunks, true, true);
    }

    public ChunkEvictionPolicy(Duration resettingPeriod, int numberChunks, boolean reportUncompletedChunkToSnapshot) {
        this(resettingPeriod, numberChunks, reportUncompletedChunkToSnapshot, true);
    }

    public ChunkEvictionPolicy(Duration resettingPeriod, int numberChunks, boolean reportUncompletedChunkToSnapshot, boolean smoothlyEvictFromOldestChunk) {
        if (resettingPeriod.isNegative() || resettingPeriod.isZero()) {
            throw new IllegalArgumentException("resettingPeriod must be a positive duration");
        }
        if (numberChunks < 2) {
            throw new IllegalArgumentException("numberChunks should be >= 2");
        }
        this.resettingPeriodMillis = resettingPeriod.toMillis();
        this.numberChunks = numberChunks;
        this.reportUncompletedChunkToSnapshot = reportUncompletedChunkToSnapshot;
        this.smoothlyEvictFromOldestChunk = smoothlyEvictFromOldestChunk;
    }

    public long getResettingPeriodMillis() {
        return resettingPeriodMillis;
    }

    public int getNumberChunks() {
        return numberChunks;
    }

    public boolean isReportUncompletedChunkToSnapshot() {
        return reportUncompletedChunkToSnapshot;
    }

    public boolean isSmoothlyEvictFromOldestChunk() {
        return smoothlyEvictFromOldestChunk;
    }

    @Override
    public String toString() {
        return "ChunkEvictionPolicy{" +
                "resettingPeriodMillis=" + resettingPeriodMillis +
                ", numberChunks=" + numberChunks +
                ", reportUncompletedChunkToSnapshot=" + reportUncompletedChunkToSnapshot +
                ", smoothlyEvictFromOldestChunk=" + smoothlyEvictFromOldestChunk +
                '}';
    }

}
