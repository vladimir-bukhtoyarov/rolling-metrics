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

import com.github.rollingmetrics.counter.SmoothlyDecayingRollingCounter;
import com.github.rollingmetrics.counter.WindowCounter;
import com.github.rollingmetrics.histogram.OverflowResolver;
import com.github.rollingmetrics.histogram.hdr.RollingHdrHistogram;
import com.github.rollingmetrics.util.Ticker;
import io.github.gcmonitor.GcMonitorConfiguration;

import java.time.Duration;
import java.util.Optional;

public interface WindowSpecification {

    CollectorWindow createWindow(long currentTimeMillis, GcMonitorConfiguration configuration);

    static WindowSpecification uniform() {
        return (currentTimeMillis, configuration) -> {
            WindowCounter counter = new UniformWindowCounter();

            RollingHdrHistogram histogram = RollingHdrHistogram.builder()
                    .neverResetReservoir()
                    .withHighestTrackableValue(configuration.getLongestTrackablePauseMillis(), OverflowResolver.REDUCE_TO_HIGHEST_TRACKABLE)
                    .withPredefinedPercentiles(configuration.getPercentiles())
                    .withSignificantDigits(configuration.getHistogramSignificantDigits())
                    .build();
            return new CollectorWindow(currentTimeMillis, Optional.empty(), counter, histogram);
        };
    }

    static WindowSpecification rollingWindow(Duration rollingWindow, Ticker ticker) {
        return (currentTimeMillis, configuration) -> {
            WindowCounter counter = new SmoothlyDecayingRollingCounter(rollingWindow, configuration.getCounterChunks(), ticker);
            RollingHdrHistogram histogram = RollingHdrHistogram.builder()
                    .withTicker(ticker)
                    .resetReservoirPeriodicallyByChunks(rollingWindow, configuration.getHistogramChunks())
                    .withHighestTrackableValue(configuration.getLongestTrackablePauseMillis(), OverflowResolver.REDUCE_TO_HIGHEST_TRACKABLE)
                    .withPredefinedPercentiles(configuration.getPercentiles())
                    .withSignificantDigits(configuration.getHistogramSignificantDigits())
                    .build();
            return new CollectorWindow(currentTimeMillis, Optional.of(rollingWindow), counter, histogram);
        };
    }

}
