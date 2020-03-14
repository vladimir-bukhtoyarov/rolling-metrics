/*
 *
 *  Copyright 2017 Vladimir Bukhtoyarov
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

package com.github.rollingmetrics.dropwizard;

import com.codahale.metrics.*;
import com.github.rollingmetrics.counter.WindowCounter;
import com.github.rollingmetrics.dropwizard.adapter.ReservoirToRollingHdrHistogramAdapter;
import com.github.rollingmetrics.dropwizard.adapter.TopMetricSet;
import com.github.rollingmetrics.histogram.hdr.RollingHdrHistogram;
import com.github.rollingmetrics.hitratio.HitRatio;
import com.github.rollingmetrics.ranking.Ranking;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Util class which provides compatibility adapters from Rolling-Metrics to Dropwizard-Metrics.
 */
public class Dropwizard {

    /**
     * Builds reservoir which can be useful for building monitoring primitives with higher level of abstraction.
     *
     * @return an instance of {@link com.codahale.metrics.Reservoir}
     */
    public static Reservoir toReservoir(RollingHdrHistogram rollingHdrHistogram) {
        return new ReservoirToRollingHdrHistogramAdapter(rollingHdrHistogram);
    }

    /**
     * Builds histogram.
     *
     * @return an instance of {@link com.codahale.metrics.Histogram}
     */
    public static Histogram toHistogram(RollingHdrHistogram rollingHdrHistogram) {
        return new Histogram(toReservoir(rollingHdrHistogram));
    }

    /**
     * Builds timer.
     *
     * @return an instance of {@link com.codahale.metrics.Timer}
     */
    public static Timer toTimer(RollingHdrHistogram rollingHdrHistogram) {
        return new Timer(toReservoir(rollingHdrHistogram));
    }

    /**
     * Converts window-counter to Dropwizard Gauge
     *
     * @param counter
     *
     * @return Dropwizard Gauge
     */
    public static Gauge<Long> toGauge(WindowCounter counter) {
        Objects.requireNonNull(counter);
        return counter::getSum;
    }

    /**
     * Converts hit-ratio to Dropwizard Gauge
     *
     * @param hitRatio
     *
     * @return Dropwizard Gauge
     */
    public static Gauge<Double> toGauge(HitRatio hitRatio) {
        Objects.requireNonNull(hitRatio);
        return hitRatio::getHitRatio;
    }

    /**
     * Creates new collection of gauges which compatible with {@link com.codahale.metrics.MetricRegistry}.
     *
     * @param name the name prefix for each gauge
     * @param ranking the target {@link Ranking}
     * @param latencyUnit the time unit to convert latency
     * @param digitsAfterDecimalPoint the number of digits after decimal point
     */
    public static MetricSet toMetricSet(String name, Ranking ranking, TimeUnit latencyUnit, int digitsAfterDecimalPoint) {
        return new TopMetricSet(name, ranking, latencyUnit, digitsAfterDecimalPoint);
    }

}
