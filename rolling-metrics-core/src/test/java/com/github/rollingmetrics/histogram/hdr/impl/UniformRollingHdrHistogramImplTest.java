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
import com.github.rollingmetrics.histogram.hdr.RollingHdrHistogramSnapshot;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

public class UniformRollingHdrHistogramImplTest {

    @Test
    public void shouldCacheSnapshot() {
        RollingHdrHistogram histogram = RollingHdrHistogram.builder()
                .neverResetReservoir()
                .build();

        histogram.update(10);
        histogram.update(20);
        RollingHdrHistogramSnapshot firstSnapshot = histogram.getSnapshot();

        histogram.update(30);
        histogram.update(40);
        RollingHdrHistogramSnapshot secondSnapshot = histogram.getSnapshot();
        assertNotSame(firstSnapshot, secondSnapshot);
        assertEquals(10, secondSnapshot.getMin());
        assertEquals(40, secondSnapshot.getMax());

        histogram.update(9);
        histogram.update(60);
        RollingHdrHistogramSnapshot thirdSnapshot = histogram.getSnapshot();
        assertNotSame(secondSnapshot, thirdSnapshot);
        assertEquals(9, thirdSnapshot.getMin());
        assertEquals(60, thirdSnapshot.getMax());
    }

    @Test
    public void testToString() {
        RollingHdrHistogram.builder()
                .neverResetReservoir()
                .build()
                .toString();
    }

}