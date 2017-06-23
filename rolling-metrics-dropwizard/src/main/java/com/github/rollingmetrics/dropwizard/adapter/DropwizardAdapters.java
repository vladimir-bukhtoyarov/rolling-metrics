/*
 *    Copyright 2017 Vladimir Bukhtoyarov
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

package com.github.rollingmetrics.dropwizard.adapter;

import com.codahale.metrics.*;
import com.github.rollingmetrics.dropwizard.adapter.ReservoirToRollingHdrHistogramAdapter;
import com.github.rollingmetrics.dropwizard.adapter.TopMetricSet;
import com.github.rollingmetrics.histogram.hdr.RollingHdrHistogram;
import com.github.rollingmetrics.top.Top;

import java.util.concurrent.TimeUnit;

/**
 * Created by vladimir.bukhtoyarov on 23.06.2017.
 */
public class DropwizardAdapters {

    /**
     * Builds reservoir which can be useful for building monitoring primitives with higher level of abstraction.
     *
     * @return an instance of {@link com.codahale.metrics.Reservoir}
     */
    public static Reservoir convertToReservoir(RollingHdrHistogram rollingHdrHistogram) {
        return new ReservoirToRollingHdrHistogramAdapter(rollingHdrHistogram);
    }

    /**
     * Builds histogram.
     *
     * @return an instance of {@link com.codahale.metrics.Histogram}
     */
    public static Histogram convertToHistogram(RollingHdrHistogram rollingHdrHistogram) {
        return new Histogram(convertToReservoir(rollingHdrHistogram));
    }

    /**
     * Builds and registers histogram.
     *
     * @param registry metric registry in which constructed histogram will be registered
     * @param name     the name under with constructed histogram will be registered in the {@code registry}
     * @return an instance of {@link com.codahale.metrics.Histogram}
     */
    public static Histogram convertToHistogramAndRegister(RollingHdrHistogram rollingHdrHistogram, MetricRegistry registry, String name) {
        Histogram histogram = convertToHistogram(rollingHdrHistogram);
        registry.register(name, histogram);
        return histogram;
    }

    /**
     * Builds timer.
     *
     * @return an instance of {@link com.codahale.metrics.Timer}
     */
    public static Timer convertToTimer(RollingHdrHistogram rollingHdrHistogram) {
        return new Timer(convertToReservoir(rollingHdrHistogram));
    }

    /**
     * Builds and registers timer.
     *
     * @param registry metric registry in which constructed histogram will be registered
     * @param name     the name under with constructed timer will be registered in the {@code registry}
     * @return an instance of {@link com.codahale.metrics.Timer}
     */
    public static Timer convertToTimerAndRegister(RollingHdrHistogram rollingHdrHistogram, MetricRegistry registry, String name) {
        Timer timer = convertToTimer(rollingHdrHistogram);
        registry.register(name, timer);
        return timer;
    }

    /**
     * Creates new collection of gauges which compatible with {@link com.codahale.metrics.MetricRegistry}.
     *
     * @param name the name prefix for each gauge
     * @param top the target {@link Top}
     * @param latencyUnit the time unit to convert latency
     * @param digitsAfterDecimalPoint the number of digits after decimal point
     */
    public static MetricSet convertTopToMetricSet(String name, Top top, TimeUnit latencyUnit, int digitsAfterDecimalPoint) {
        return new TopMetricSet(name, top, latencyUnit, digitsAfterDecimalPoint);
    }

}
