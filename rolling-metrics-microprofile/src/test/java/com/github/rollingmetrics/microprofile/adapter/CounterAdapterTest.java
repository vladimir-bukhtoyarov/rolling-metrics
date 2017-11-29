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

package com.github.rollingmetrics.microprofile.adapter;

import com.github.rollingmetrics.counter.WindowCounter;
import com.github.rollingmetrics.microprofile.MicroProfile;
import com.github.rollingmetrics.retention.RetentionPolicy;
import org.eclipse.microprofile.metrics.Gauge;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class CounterAdapterTest {

    @Test(expected = NullPointerException.class)
    public void shouldNotAllowToConvertNullCounter() {
        MicroProfile.toGauge((WindowCounter) null);
    }

    @Test
    public void shouldProperlyReturnValue() {
        WindowCounter counter = RetentionPolicy.resetOnSnapshot().newCounter();
        counter.add(100);
        Gauge<Long> gauge = MicroProfile.toGauge(counter);
        assertEquals(100L, gauge.getValue().longValue());
    }

}
