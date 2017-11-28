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

package com.github.rollingmetrics.histogram.hdr.impl;

import com.github.rollingmetrics.histogram.hdr.RollingHdrHistogram;
import com.github.rollingmetrics.histogram.hdr.RollingSnapshot;
import com.github.rollingmetrics.retention.RetentionPolicy;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotSame;

public class ResetOnSnapshotRollingHdrHistogramImplTest {

    @Test
    public void shouldCacheSnapshot() {
        RollingHdrHistogram histogram = RetentionPolicy
                .resetOnSnapshot()
                .newRollingHdrHistogramBuilder()
                .build();

        histogram.update(10);
        histogram.update(20);
        RollingSnapshot firstSnapshot = histogram.getSnapshot();

        histogram.update(30);
        histogram.update(40);
        RollingSnapshot secondSnapshot = histogram.getSnapshot();
        assertNotSame(firstSnapshot, secondSnapshot);
        assertEquals(30, secondSnapshot.getMin());
        assertEquals(40, secondSnapshot.getMax());

        histogram.update(50);
        histogram.update(60);
        RollingSnapshot thirdSnapshot = histogram.getSnapshot();
        assertNotSame(secondSnapshot, thirdSnapshot);
        assertEquals(50, thirdSnapshot.getMin());
        assertEquals(60, thirdSnapshot.getMax());
    }

    @Test
    public void testToString() {
        RetentionPolicy
                .resetOnSnapshot()
                .newRollingHdrHistogramBuilder()
                .build()
                .toString();
    }

}