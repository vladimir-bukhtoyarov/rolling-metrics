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

package com.github.rollingmetrics.dropwizard;import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import com.github.rollingmetrics.dropwizard.adapter.DropwizardAdapters;
import com.github.rollingmetrics.top.TopTestData;
import com.github.rollingmetrics.top.Top;
import com.github.rollingmetrics.top.impl.TopTestUtil;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class TopMetricSetTest {

    private Top top = Top.builder(3).withSnapshotCachingDuration(Duration.ZERO).build();

    @Test(expected = IllegalArgumentException.class)
    public void shouldDisallowNullName() {
        DropwizardAdapters.convertTopToMetricSet(null, top, TimeUnit.MILLISECONDS, 3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldDisallowEmptyName() {
        DropwizardAdapters.convertTopToMetricSet("", top, TimeUnit.MILLISECONDS, 3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldDisallowNullTop() {
        DropwizardAdapters.convertTopToMetricSet("my-top", null, TimeUnit.MILLISECONDS, 3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldDisallowNullLatencyUnit() {
        DropwizardAdapters.convertTopToMetricSet("my-top", top, null, 3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldDisallowNegativeDigitsAfterDecimalPoint() {
        DropwizardAdapters.convertTopToMetricSet("my-top", top, TimeUnit.MILLISECONDS, -1);
    }

    @Test
    public void shouldAddLatencyUnitGauge() {
        for (TimeUnit timeUnit: TimeUnit.values()) {
            MetricSet metricSet = DropwizardAdapters.convertTopToMetricSet("my-top", top, timeUnit, 3);
            Map<String, Metric> metrics = metricSet.getMetrics();
            Gauge<String> timeUnitGauge = (Gauge<String>) metrics.get("my-top.latencyUnit");
            Assert.assertNotNull(timeUnitGauge);
            assertEquals(timeUnit.toString(), timeUnitGauge.getValue());
        }
    }

    @Test
    public void testDescriptionGauges() {
        MetricSet metricSet = DropwizardAdapters.convertTopToMetricSet("my-top", top, TimeUnit.MILLISECONDS, 3);
        checkDescriptions(metricSet, "my-top", "", "", "");

        TopTestUtil.update(top, TopTestData.first);
        checkDescriptions(metricSet, "my-top", TopTestData.first.getQueryDescription(), "", "");

        TopTestUtil.update(top, TopTestData.second);
        checkDescriptions(metricSet, "my-top", TopTestData.second.getQueryDescription(), TopTestData.first.getQueryDescription(), "");

        TopTestUtil.update(top, TopTestData.third);
        checkDescriptions(metricSet, "my-top", TopTestData.third.getQueryDescription(), TopTestData.second.getQueryDescription(), TopTestData.first.getQueryDescription());
    }

    @Test
    public void testValueGauges() {
        MetricSet metricSet = DropwizardAdapters.convertTopToMetricSet("my-top", top, TimeUnit.MILLISECONDS, 3);
        checkValues(metricSet, "my-top", 3, 0.0d, 0.0d, 0.0d);

        top.update(0, 13_345_456, TimeUnit.NANOSECONDS, () -> "SELECT * FROM USERS");
        checkValues(metricSet, "my-top", 3, 13.345d, 0.0d, 0.0d);


        top.update(0, 11_666_957, TimeUnit.NANOSECONDS, () -> "SELECT * FROM USERS");
        checkValues(metricSet, "my-top", 3, 13.345d, 11.666d, 0.0d);

        top.update(0, 2_004_123, TimeUnit.NANOSECONDS, () -> "SELECT * FROM DUAL");
        checkValues(metricSet, "my-top", 3, 13.345d, 11.666d, 2.004d);
    }

    private void checkDescriptions(MetricSet metricSet, String name, String... requiredDescriptions) {
        for (int i = 0; i < requiredDescriptions.length; i++) {
            String requiredDescription = requiredDescriptions[i];
            Gauge<String> gauge = (Gauge<String>) metricSet.getMetrics().get(name + "." + i + ".description");
            String description = gauge.getValue();
            Assert.assertEquals(requiredDescription, description);
        }
    }

    private void checkValues(MetricSet metricSet, String name, int digitsAfterDecimalPoint, double... requiredLatencies) {
        for (int i = 0; i < requiredLatencies.length; i++) {
            BigDecimal requiredLatency = new BigDecimal(requiredLatencies[i]).setScale(digitsAfterDecimalPoint, RoundingMode.CEILING);
            Gauge<BigDecimal> gauge = (Gauge<BigDecimal>) metricSet.getMetrics().get(name + "." + i + ".latency");
            BigDecimal latency = gauge.getValue();
            Assert.assertEquals(requiredLatency, latency);
        }
    }

}