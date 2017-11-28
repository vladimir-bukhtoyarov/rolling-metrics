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

import com.github.rollingmetrics.histogram.hdr.impl.EmptyRollingSnapshot;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class EmptyRollingSnapshotTest {

    @Test
    public void testGetValue() throws Exception {
        assertEquals(0.0, EmptyRollingSnapshot.INSTANCE.getValue(50.0));
        assertEquals(0.0, EmptyRollingSnapshot.INSTANCE.getValue(60.0));
    }

    @Test
    public void testGetValues() throws Exception {
        long[] values = EmptyRollingSnapshot.INSTANCE.getValues();
        assertEquals(0, values.length);
    }

    @Test
    public void testSize() throws Exception {
        assertEquals(0, EmptyRollingSnapshot.INSTANCE.size());
    }

    @Test
    public void testGetMax() throws Exception {
        assertEquals(0, EmptyRollingSnapshot.INSTANCE.getMax());
    }

    @Test
    public void testGetMean() throws Exception {
        assertEquals(0.0, EmptyRollingSnapshot.INSTANCE.getMean());
    }

    @Test
    public void testGetMin() throws Exception {
        assertEquals(0, EmptyRollingSnapshot.INSTANCE.getMin());
    }

    @Test
    public void testGetStdDev() throws Exception {
        assertEquals(0.0, EmptyRollingSnapshot.INSTANCE.getStdDev());
    }

}