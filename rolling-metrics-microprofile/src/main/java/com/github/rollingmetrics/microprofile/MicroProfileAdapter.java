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

package com.github.rollingmetrics.microprofile;


import com.github.rollingmetrics.counter.WindowCounter;
import com.github.rollingmetrics.histogram.hdr.RollingHdrHistogram;
import com.github.rollingmetrics.hitratio.HitRatio;
import com.github.rollingmetrics.microprofile.adapter.MicroProfileHistogramAdapter;
import com.github.rollingmetrics.microprofile.adapter.MicroProfileTimerAdapter;
import com.github.rollingmetrics.microprofile.adapter.TopUtil;
import com.github.rollingmetrics.top.Top;
import org.eclipse.microprofile.metrics.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Util class which provides the adapters from Rolling-Metrics to Eclipse MicroProfile-Metrics.
 */
public class MicroProfileAdapter {

    /**
     * Creates adpater from rolling histogram to Eclipse-MicroProfile histogram.
     *
     * @return an instance of {@link Histogram}
     */
    public static Histogram convertToHistogram(RollingHdrHistogram rollingHdrHistogram) {
        return new MicroProfileHistogramAdapter(rollingHdrHistogram);
    }

    /**
     * Builds and registers histogram.
     *
     * @param registry metric registry in which constructed histogram will be registered
     * @param name     the name under with constructed histogram will be registered in the {@code registry}
     * @return an instance of {@link Histogram}
     */
    public static Histogram convertToHistogramAndRegister(RollingHdrHistogram rollingHdrHistogram, MetricRegistry registry, String name) {
        Histogram histogram = convertToHistogram(rollingHdrHistogram);
        registry.register(name, histogram);
        return histogram;
    }

    /**
     * Builds timer.
     *
     * @return an instance of {@link Timer}
     */
    public static Timer convertToTimer(RollingHdrHistogram rollingHdrHistogram, Meter meter) {
        return new MicroProfileTimerAdapter(rollingHdrHistogram, meter);
    }

    /**
     * Builds and registers timer.
     *
     *
     * @param rollingHdrHistogram
     * @param meter
     * @param registry metric registry in which constructed histogram will be registered
     * @param name     the name under with constructed timer will be registered in the {@code registry}
     * @return an instance of {@link Timer}
     */
    public static Timer convertToTimerAndRegister(RollingHdrHistogram rollingHdrHistogram, Meter meter, MetricRegistry registry, String name) {
        Timer timer = convertToTimer(rollingHdrHistogram, meter);
        registry.register(name, timer);
        return timer;
    }

    public Gauge<Long> convertCounterToGauge(WindowCounter counter) {
        return counter::getSum;
    }

    public Gauge<Long> convertCounterToGaugeAndRegister(WindowCounter counter, MetricRegistry registry, String name) {
        Gauge<Long> gauge = counter::getSum;
        registry.register(name, gauge);
        return gauge;
    }

    public Gauge<Double> convertHitratioToGauge(HitRatio hitRatio) {
        return hitRatio::getHitRatio;
    }

    public Gauge<Double> convertHitratioToGaugeAndRegister(HitRatio hitRatio, MetricRegistry registry, String name) {
        Gauge<Double> gauge = hitRatio::getHitRatio;
        registry.register(name, gauge);
        return gauge;
    }

    /**
     * Creates bunch of gauges from Top and does registration of each gauge in {@link MetricRegistry}.
     *
     * <p>
     * <p><b>Sample Usage:</b>
     * <pre><code>
     * Top top = Top.builder(3).resetAllPositionsOnSnapshot().build();
     * MicroProfileAdapter.registerTop(top, TimeUnit.MILLISECONDS, 5, registry, "my-top");
     * </code></pre>
     * The code above creates 7 gauges with following names:
     * <ul>
     *   <li>my-top.latencyUnit</li>
     *   <li>my-top.0.latency</li>
     *   <li>my-top.0.description</li>
     *   <li>my-top.1.latency</li>
     *   <li>my-top.1.description</li>
     *   <li>my-top.2.latency</li>
     *   <li>my-top.2.description</li>
     * </ul>
     * The "latency" gauges have {@link BigDecimal} type, the "latencyUnit" and "description" gauges have {@link String} type.
     * The number in the gauge name represents position in the top in descending order, the "0" is the slowest query.
     *
     * @param top the target {@link Top}
     * @param latencyUnit the time unit to convert latency
     * @param digitsAfterDecimalPoint the number of digits after decimal point
     * @param registry
     * @param name the name prefix for each gauge
     */
    public static void registerTop(Top top, TimeUnit latencyUnit, int digitsAfterDecimalPoint, MetricRegistry registry, String name) {
        Map<String, Gauge<?>> gauges = TopUtil.convertTop(name, top, latencyUnit, digitsAfterDecimalPoint);
        gauges.forEach(registry::register);
    }

}
