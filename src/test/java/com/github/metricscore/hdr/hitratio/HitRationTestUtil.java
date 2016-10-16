/*
 *    Copyright 2016 Vladimir Bukhtoyarov
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

package com.github.metricscore.hdr.hitratio;

import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static junit.framework.TestCase.fail;

public class HitRationTestUtil {

    public static void checkIllegalApiUsageDetection(HitRatio hitRatio) {
        try {
            hitRatio.update(-1, 2);
            fail("negative hit count should be forbidden");
        } catch (IllegalArgumentException e){
            // ok
        }

        try {
            hitRatio.update(0, 0);
            fail("non-positive totalCount should be forbidden");
        } catch (IllegalArgumentException e){
            // ok
        }

        try {
            hitRatio.update(0, -1);
            fail("non-positive totalCount should be forbidden");
        } catch (IllegalArgumentException e){
            // ok
        }

        try {
            hitRatio.update(10, 5);
            fail("should checkIllegalApiUsageDetection that hitCount <= totalCount");
        } catch (IllegalArgumentException e){
            // ok
        }
    }

    public static void runInParallel(HitRatio hitRatio, Duration duration) throws InterruptedException {
        AtomicBoolean stopFlag = new AtomicBoolean(false);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                stopFlag.set(true);
            }
        }, duration.toMillis());

        Thread[] threads = new Thread[Runtime.getRuntime().availableProcessors() * 2];
        final CountDownLatch latch = new CountDownLatch(threads.length);
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                try {
                    // update reservoir 100 times and take snapshot on each cycle
                    while (!stopFlag.get()) {
                        for (int j = 1; j <= 10; j++) {
                            int randomInt = ThreadLocalRandom.current().nextInt(j);
                            hitRatio.update(randomInt, randomInt + 1);
                        }
                        hitRatio.getHitRatio();
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

}