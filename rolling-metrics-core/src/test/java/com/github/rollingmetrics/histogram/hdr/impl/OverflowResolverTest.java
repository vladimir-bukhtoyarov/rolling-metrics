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

package com.github.rollingmetrics.histogram.hdr.impl;

import com.github.rollingmetrics.histogram.OverflowResolver;
import com.github.rollingmetrics.histogram.hdr.RollingHdrHistogram;
import com.github.rollingmetrics.histogram.hdr.RollingSnapshot;
import com.github.rollingmetrics.retention.RetentionPolicy;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class OverflowResolverTest {

    @Test
    public void testSkipBigValues() {
        RollingHdrHistogram histogram = RetentionPolicy.uniform()
                .newRollingHdrHistogramBuilder()
                .withHighestTrackableValue(100, OverflowResolver.SKIP).
                build();

        histogram.update(101);
        RollingSnapshot snapshot = histogram.getSnapshot();
        assertEquals(0, snapshot.getMax());

        histogram.update(100);
        snapshot = histogram.getSnapshot();
        assertEquals(100, snapshot.getMax());

        histogram.update(99);
        snapshot = histogram.getSnapshot();
        assertEquals(99, snapshot.getMin());
    }

    @Test
    public void testReduceBigValuesToMax() {
        RollingHdrHistogram histogram = RetentionPolicy.uniform()
                .newRollingHdrHistogramBuilder()
                .withHighestTrackableValue(100, OverflowResolver.REDUCE_TO_HIGHEST_TRACKABLE)
                .build();

        histogram.update(101);
        RollingSnapshot snapshot = histogram.getSnapshot();
        assertEquals(100, snapshot.getMax());

        histogram.update(100);
        snapshot = histogram.getSnapshot();
        assertEquals(100, snapshot.getMax());

        histogram.update(99);
        snapshot = histogram.getSnapshot();
        assertEquals(99, snapshot.getMin());
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testPassThruBigValues() {
        RollingHdrHistogram histogram = RetentionPolicy.uniform()
                .newRollingHdrHistogramBuilder()
                .withHighestTrackableValue(100, OverflowResolver.PASS_THRU)
                .build();
        histogram.update(100000);
    }

    @Test
    public void testPassThruBigValues2() {
        RollingHdrHistogram histogram = RetentionPolicy.uniform()
                .newRollingHdrHistogramBuilder()
                .withHighestTrackableValue(100, OverflowResolver.PASS_THRU)
                .build();
        histogram.update(101);
        RollingSnapshot snapshot = histogram.getSnapshot();
        assertEquals(101, snapshot.getMax());
    }

}
