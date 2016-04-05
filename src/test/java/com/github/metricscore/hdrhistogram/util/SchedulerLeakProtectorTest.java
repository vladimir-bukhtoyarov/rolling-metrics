package com.github.metricscore.hdrhistogram.util;

import org.junit.Test;

import java.lang.ref.WeakReference;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static org.junit.Assert.assertTrue;

public class SchedulerLeakProtectorTest {

    @Test(timeout = 5000)
    public void testScheduleAtFixedRate() throws InterruptedException {
        WeakReference<String> baitReference = new WeakReference<>("bait");
        System.out.println(baitReference);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Consumer<String> consumer = string ->countDownLatch.countDown();
        ScheduledFuture<?> future = SchedulerLeakProtector.scheduleAtFixedRate(scheduler, baitReference, consumer, 0, 2, TimeUnit.SECONDS);

        // wait
        countDownLatch.await();

        // remove strong reference and perform GC
        baitReference.clear();
        Thread.sleep(3000);
        assertTrue(future.isCancelled());

        System.out.println(future.isCancelled());
        scheduler.shutdown();
    }

}