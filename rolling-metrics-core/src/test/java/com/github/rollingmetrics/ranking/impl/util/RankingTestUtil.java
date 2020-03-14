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
import com.github.rollingmetrics.ranking.Ranking;
import com.github.rollingmetrics.ranking.impl.recorder.SingleThreadedRanking;
import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.rollingmetrics.ranking.impl.recorder.SingleThreadedRanking.UpdateResult.INSERTED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RankingTestUtil {

    public static void testCommonScenarios(int size, Ranking ranking, long latencyThresholdNanos, int maxDescriptionLength) {
        assertEquals(size, ranking.getSize());
        negativeLatencyShouldBeIgnored(ranking);
        tooShortLatencyShouldBeIgnored(ranking, latencyThresholdNanos);
    }

    public static boolean update(SingleThreadedRanking collector, Position position) {
        return collector.update(position.getWeight(), position.getIdentity()) == INSERTED;
    }

    public static void update(Ranking ranking, Position position) {
        ranking.update(position.getWeight(), position.getIdentity());
    }

    public static void checkOrder(Ranking ranking, Position... positions) {
        TestCase.assertEquals(Arrays.asList(positions), ranking.getPositionsInDescendingOrder());
    }

    public static void assertEmpty(Ranking ranking) {
        assertEquals(Collections.emptyList(), ranking.getPositionsInDescendingOrder());
    }

    public static void runInParallel(Ranking ranking, long durationMillis, long minValue, long maxValue) throws InterruptedException {
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
                            ranking.update(latency, "" + latency);
                        }
                        ranking.getPositionsInDescendingOrder();
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

    private static void negativeLatencyShouldBeIgnored(Ranking ranking) {
        ranking.update(-1, "SELECT * FROM DUAL");
        assertTrue(ranking.getPositionsInDescendingOrder().isEmpty());
    }

    private static void tooShortLatencyShouldBeIgnored(Ranking ranking, long latencyThresholdNanos) {
        ranking.update(latencyThresholdNanos - 1, "SELECT * FROM DUAL");
        assertTrue(ranking.getPositionsInDescendingOrder().isEmpty());
    }

}
