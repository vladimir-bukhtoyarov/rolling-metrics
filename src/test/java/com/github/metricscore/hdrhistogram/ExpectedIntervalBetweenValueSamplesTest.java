/*
 *
 *  Copyright 2016 Vladimir Bukhtoyarov
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

package com.github.metricscore.hdrhistogram;

import com.codahale.metrics.Histogram;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;


public class ExpectedIntervalBetweenValueSamplesTest {

    @Test
    public void expectedIntervalBetweenValueSamples() {
        Histogram histogram = new HdrBuilder().withExpectedIntervalBetweenValueSamples(100).buildHistogram();
        for (int i = 1; i <= 100; i++) {
            histogram.update(i);
        }
        assertEquals(75.0, histogram.getSnapshot().get75thPercentile());
        assertEquals(99.0, histogram.getSnapshot().get99thPercentile());

        histogram.update(10000);
        assertEquals(5023.0, histogram.getSnapshot().get75thPercentile());
        assertEquals(9855.0, histogram.getSnapshot().get99thPercentile());
    }

}
