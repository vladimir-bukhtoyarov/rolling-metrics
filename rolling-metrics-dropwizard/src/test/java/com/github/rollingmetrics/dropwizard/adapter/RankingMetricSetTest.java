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

package com.github.rollingmetrics.dropwizard.adapter;import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import com.github.rollingmetrics.dropwizard.Dropwizard;
import com.github.rollingmetrics.ranking.Ranking;
import com.github.rollingmetrics.ranking.impl.util.RankingTestData;
import com.github.rollingmetrics.ranking.impl.util.RankingTestUtil;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class RankingMetricSetTest {

    private Ranking ranking = Ranking.builder(3).withSnapshotCachingDuration(Duration.ZERO).build();

    @Test(expected = IllegalArgumentException.class)
    public void shouldDisallowNullName() {
        Dropwizard.toMetricSet(null, ranking, TimeUnit.MILLISECONDS, 3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldDisallowEmptyName() {
        Dropwizard.toMetricSet("", ranking, TimeUnit.MILLISECONDS, 3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldDisallowNullTop() {
        Dropwizard.toMetricSet("my-top", null, TimeUnit.MILLISECONDS, 3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldDisallowNullLatencyUnit() {
        Dropwizard.toMetricSet("my-top", ranking, null, 3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldDisallowNegativeDigitsAfterDecimalPoint() {
        Dropwizard.toMetricSet("my-top", ranking, TimeUnit.MILLISECONDS, -1);
    }

    @Test
    public void shouldAddLatencyUnitGauge() {
        for (TimeUnit timeUnit: TimeUnit.values()) {
            MetricSet metricSet = Dropwizard.toMetricSet("my-top", ranking, timeUnit, 3);
            Map<String, Metric> metrics = metricSet.getMetrics();
            Gauge<String> timeUnitGauge = (Gauge<String>) metrics.get("my-top.latencyUnit");
            Assert.assertNotNull(timeUnitGauge);
            assertEquals(timeUnit.toString(), timeUnitGauge.getValue());
        }
    }

    @Test
    public void testDescriptionGauges() {
        MetricSet metricSet = Dropwizard.toMetricSet("my-top", ranking, TimeUnit.MILLISECONDS, 3);
        checkDescriptions(metricSet, "my-top", "", "", "");

        RankingTestUtil.update(ranking, RankingTestData.first);
        checkDescriptions(metricSet, "my-top", (String) RankingTestData.first.getIdentity(), "", "");

        RankingTestUtil.update(ranking, RankingTestData.second);
        checkDescriptions(metricSet, "my-top", (String) RankingTestData.second.getIdentity(), (String) RankingTestData.first.getIdentity(), "");

        RankingTestUtil.update(ranking, RankingTestData.third);
        checkDescriptions(metricSet, "my-top", (String) RankingTestData.third.getIdentity(), (String) RankingTestData.second.getIdentity(), (String) RankingTestData.first.getIdentity());
    }

    @Test
    public void testValueGauges() {
        MetricSet metricSet = Dropwizard.toMetricSet("my-top", ranking, TimeUnit.MILLISECONDS, 3);
        checkValues(metricSet, "my-top", 3, 0.0d, 0.0d, 0.0d);

        ranking.update(13_345_456, "SELECT * FROM USERS");
        checkValues(metricSet, "my-top", 3, 13.345d, 0.0d, 0.0d);


        ranking.update(11_666_957, "SELECT * FROM USERS");
        checkValues(metricSet, "my-top", 3, 13.345d, 11.666d, 0.0d);

        ranking.update(2_004_123, "SELECT * FROM DUAL");
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