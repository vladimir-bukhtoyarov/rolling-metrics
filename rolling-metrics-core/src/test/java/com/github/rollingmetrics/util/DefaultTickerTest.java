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

package com.github.rollingmetrics.util;

import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static junit.framework.TestCase.assertEquals;

public class DefaultTickerTest {

    @Test
    public void testStableMillisOnInit() {
        testStableMillisOnInit(0, 0);
        testStableMillisOnInit(Long.MAX_VALUE, 0);
        testStableMillisOnInit(Long.MIN_VALUE, 0);
        testStableMillisOnInit(-1 * TimeUnit.MINUTES.toNanos(45), 0);
    }

    private void testStableMillisOnInit(long initialTimeNanos, long requiredStableMillisAtInitialization) {
        AtomicLong timeNanos = new AtomicLong(initialTimeNanos);
        DefaultTicker ticker = new DefaultTicker() {
            @Override
            public long nanoTime() {
                return timeNanos.get();
            }
        };
        long actualStableMillisAtInitialization = ticker.stableMilliseconds();
        assertEquals(requiredStableMillisAtInitialization, actualStableMillisAtInitialization);

        timeNanos.set(initialTimeNanos + TimeUnit.HOURS.toNanos(1));
        assertEquals(requiredStableMillisAtInitialization + TimeUnit.HOURS.toMillis(1), ticker.stableMilliseconds());
    }

    @Test
    public void testStableMillisWhenChangingSignum() {
        checkStableMillis(1, Long.MAX_VALUE, Long.MAX_VALUE / 1_000_000);
        checkStableMillis(Long.MAX_VALUE, TimeUnit.MINUTES.toNanos(45), TimeUnit.MINUTES.toMillis(45));
        checkStableMillis(-1, 1, 0);
        checkStableMillis(-1, 1_000_000, 1);
    }

    private void checkStableMillis(long initialTimeNanos, long deltaNanos, long requiredStableMillisInTheEnd) {
        AtomicLong timeNanos = new AtomicLong(initialTimeNanos);
        DefaultTicker ticker = new DefaultTicker() {
            @Override
            public long nanoTime() {
                return timeNanos.get();
            }
        };

        timeNanos.set(initialTimeNanos + deltaNanos);
        assertEquals(requiredStableMillisInTheEnd, ticker.stableMilliseconds());
    }

}