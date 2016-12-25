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

package com.github.rollingmetrics.top.impl;

import com.github.rollingmetrics.counter.WindowCounter;
import com.github.rollingmetrics.top.Position;
import com.github.rollingmetrics.top.TestData;
import com.github.rollingmetrics.top.Top;
import com.github.rollingmetrics.top.impl.collector.PositionCollector;
import com.github.rollingmetrics.top.impl.recorder.PositionRecorder;
import com.github.rollingmetrics.top.TestData;
import com.github.rollingmetrics.top.Top;
import junit.framework.TestCase;
import org.junit.Assert;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TopTestUtil {

    public static void testCommonScenarios(int size, Top top, long latencyThresholdNanos, int maxDescriptionLength) {
        assertEquals(size, top.getSize());
        negativeLatencyShouldBeIgnored(top);
        tooShortLatencyShouldBeIgnored(top, latencyThresholdNanos);
        tooLongDescriptionShouldBeReduced(top, latencyThresholdNanos, maxDescriptionLength);
    }

    public static void update(Top top, Position position) {
        top.update(position.getTimestamp(), position.getLatencyTime(), position.getLatencyUnit(), position::getQueryDescription);
    }


    public static void checkOrder(Top top, Position... positions) {
        TestCase.assertEquals(Arrays.asList(positions), top.getPositionsInDescendingOrder());
    }

    public static void assertEmpty(Top top) {
        Assert.assertEquals(Collections.emptyList(), top.getPositionsInDescendingOrder());
    }

    public static void runInParallel(Top top, long durationMillis, long minValue, long maxValue) throws InterruptedException {
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        Thread[] threads = new Thread[Runtime.getRuntime().availableProcessors() * 2];
        final CountDownLatch latch = new CountDownLatch(threads.length);
        long start = System.currentTimeMillis();
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                try {
                    // update top 10 times and take snapshot on each cycle
                    while (errorRef.get() == null && System.currentTimeMillis() - start < durationMillis) {
                        for (int j = 1; j <= 10; j++) {
                            long latency = minValue + ThreadLocalRandom.current().nextLong(maxValue - minValue);
                            top.update(System.currentTimeMillis(), latency, TimeUnit.NANOSECONDS, () -> "" + latency);
                        }
                        top.getPositionsInDescendingOrder();
                    }
                } catch (Exception e){
                    e.printStackTrace();
                    errorRef.set(e);
                } finally {
                    latch.countDown();
                }
            });
            threads[i].setDaemon(true);
            threads[i].start();
        }
        latch.await();
        //latch.await(duration.toMillis() + 4000, TimeUnit.MILLISECONDS);
        if (latch.getCount() > 0) {
            throw new IllegalStateException("" + latch.getCount() + " was not completed");
        }
        if (errorRef.get() != null) {
            throw new RuntimeException(errorRef.get());
        }
    }

    private static void negativeLatencyShouldBeIgnored(Top top) {
        top.update(System.currentTimeMillis(), -1, TimeUnit.MILLISECONDS, () -> "SELECT * FROM DUAL");
        assertTrue(top.getPositionsInDescendingOrder().isEmpty());
    }

    private static void tooShortLatencyShouldBeIgnored(Top top, long latencyThresholdNanos) {
        top.update(System.currentTimeMillis(), latencyThresholdNanos - 1, TimeUnit.NANOSECONDS, () -> "SELECT * FROM DUAL");
        assertTrue(top.getPositionsInDescendingOrder().isEmpty());
    }

    private static void tooLongDescriptionShouldBeReduced(Top top, long latencyThresholdNanos, int maxDescriptionLength) {
        Supplier<String> longDescription = () -> TestData.generateString(maxDescriptionLength * 2);
        top.update(System.currentTimeMillis(), latencyThresholdNanos, TimeUnit.NANOSECONDS, longDescription);
        Position position = top.getPositionsInDescendingOrder().get(0);
        assertEquals(maxDescriptionLength, position.getQueryDescription().length());
    }

}
