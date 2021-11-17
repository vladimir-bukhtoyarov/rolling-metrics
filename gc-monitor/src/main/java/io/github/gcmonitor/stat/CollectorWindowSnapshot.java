/*
 *  Copyright 2017 Vladimir Bukhtoyarov
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.github.gcmonitor.stat;

import com.github.rollingmetrics.histogram.hdr.RollingSnapshot;

/**
 * Created by vladimir.bukhtoyarov on 20.03.2017.
 */
public class CollectorWindowSnapshot {

    private final RollingSnapshot pauseHistogramSnapshot;
    private final long millisSpentInGc;
    private final double percentageSpentInGc;
    private final long totalPauseCount;

    public CollectorWindowSnapshot(long totalPauseCount, RollingSnapshot pauseHistogramSnapshot, long millisSpentInGc, double percentageSpentInGc) {
        this.pauseHistogramSnapshot = pauseHistogramSnapshot;
        this.millisSpentInGc = millisSpentInGc;
        this.percentageSpentInGc = percentageSpentInGc;
        this.totalPauseCount = totalPauseCount;
    }

    public long getTotalPauseCount() {
        return totalPauseCount;
    }

    public double getPercentageSpentInGc() {
        return percentageSpentInGc;
    }

    public long getMillisSpentInGc() {
        return millisSpentInGc;
    }

    public RollingSnapshot getPauseHistogramSnapshot() {
        return pauseHistogramSnapshot;
    }

    @Override
    public String toString() {
        return "CollectorWindowSnapshot{" +
                "pauseHistogramSnapshot=" + pauseHistogramSnapshot +
                ", millisSpentInGc=" + millisSpentInGc +
                ", percentageSpentInGc=" + percentageSpentInGc +
                ", totalPauseCount=" + totalPauseCount +
                '}';
    }
}
