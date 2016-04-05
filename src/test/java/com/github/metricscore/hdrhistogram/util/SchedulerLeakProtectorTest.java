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

package com.github.metricscore.hdrhistogram.util;

import org.junit.Test;

import java.lang.ref.WeakReference;
import java.util.concurrent.*;
import java.util.function.Consumer;

import static org.junit.Assert.assertTrue;

public class SchedulerLeakProtectorTest {

    @Test(timeout = 5000)
    public void testScheduleAtFixedRate() throws InterruptedException {
        WeakReference<String> baitReference = new WeakReference<>("bait");

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Consumer<String> consumer = string ->countDownLatch.countDown();
        ScheduledFuture<?> future = SchedulerLeakProtector.scheduleAtFixedRate(scheduler, baitReference, consumer, 0, 2, TimeUnit.SECONDS);

        countDownLatch.await();

        baitReference.clear();
        Thread.sleep(3000);
        assertTrue(future.isCancelled());

        scheduler.shutdown();
    }

}