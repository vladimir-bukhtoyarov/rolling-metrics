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

package com.github.metricscore.hdr.counter;

import com.codahale.metrics.Clock;
import com.github.metricscore.hdr.ChunkEvictionPolicy;
import com.github.metricscore.hdr.MockClock;
import junit.framework.TestCase;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by vermut on 05.09.16.
 */
public class ResetByChunksCounterTest extends TestCase {

    @Test
    public void testSmoothlyInvalidateOldestChunk() throws Exception {
        AtomicLong timeMillis = new AtomicLong();
        Clock clock = MockClock.mock(timeMillis);
        ChunkEvictionPolicy evictionPolicy = new ChunkEvictionPolicy(Duration.ofSeconds(1), 3, true, true);
        ResetByChunksCounter counter = new ResetByChunksCounter(evictionPolicy, clock);

        counter.add(100);
        assertEquals(100, counter.getSum());

        timeMillis.addAndGet(2500); // 2500
        counter.add(100);
        //assertEquals(50, counter.getSum());
    }

}