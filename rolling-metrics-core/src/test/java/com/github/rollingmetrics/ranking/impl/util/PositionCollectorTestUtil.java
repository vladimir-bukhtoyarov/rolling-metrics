/*
 *
 *  Copyright 2020 Vladimir Bukhtoyarov
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

package com.github.rollingmetrics.ranking.impl.util;

import com.github.rollingmetrics.ranking.Position;
import com.github.rollingmetrics.ranking.impl.recorder.SingleThreadedRanking;
import org.junit.Assert;

import java.util.Arrays;
import java.util.Collections;

import static junit.framework.TestCase.assertEquals;

public class PositionCollectorTestUtil {

    public static void checkOrder(SingleThreadedRanking collector, Position... positions) {
        assertEquals(Arrays.asList(positions), collector.getPositionsInDescendingOrder());
    }

    public static void assertEmpty(SingleThreadedRanking collector) {
        Assert.assertEquals(Collections.emptyList(), collector.getPositionsInDescendingOrder());
    }

}
