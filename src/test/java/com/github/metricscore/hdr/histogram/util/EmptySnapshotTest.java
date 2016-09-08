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

package com.github.metricscore.hdr.histogram.util;

import junit.framework.TestCase;

public class EmptySnapshotTest extends TestCase {

    public void testGetValue() throws Exception {
        assertEquals(0.0, EmptySnapshot.INSTANCE.getValue(50.0));
        assertEquals(0.0, EmptySnapshot.INSTANCE.getValue(60.0));
    }

    public void testGetValues() throws Exception {
        long[] values = EmptySnapshot.INSTANCE.getValues();
        assertEquals(0, values.length);
    }

    public void testSize() throws Exception {
        assertEquals(0, EmptySnapshot.INSTANCE.size());
    }

    public void testGetMax() throws Exception {
        assertEquals(0, EmptySnapshot.INSTANCE.getMax());
    }

    public void testGetMean() throws Exception {
        assertEquals(0.0, EmptySnapshot.INSTANCE.getMean());
    }

    public void testGetMin() throws Exception {
        assertEquals(0, EmptySnapshot.INSTANCE.getMin());
    }

    public void testGetStdDev() throws Exception {
        assertEquals(0.0, EmptySnapshot.INSTANCE.getStdDev());
    }

    public void testDump() throws Exception {
        EmptySnapshot.INSTANCE.dump(System.out);
    }

}