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

import com.codahale.metrics.Gauge;
import com.github.rollingmetrics.dropwizard.Dropwizard;
import com.github.rollingmetrics.hitratio.HitRatio;
import com.github.rollingmetrics.retention.RetentionPolicy;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class HitRatioAdapterTest {

    @Test(expected = NullPointerException.class)
    public void shouldNotAllowToConvertNullHitRatio() {
        Dropwizard.toGauge((HitRatio) null);
    }

    @Test
    public void shouldProperlyReturnValue() {
        HitRatio hitRatio = RetentionPolicy.resetOnSnapshot().newHitRatio();
        hitRatio.update(50, 100);
        Gauge<Double> gauge = Dropwizard.toGauge(hitRatio);
        assertEquals(0.5d, gauge.getValue());
    }

}
