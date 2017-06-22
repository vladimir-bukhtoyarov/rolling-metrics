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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static junit.framework.TestCase.assertSame;

public class SingleThreadExecutorTest {

    private SingleThreadExecutor executor = new SingleThreadExecutor(new DaemonThreadFactory("xyz"));

    @Test(timeout = 10000)
    public void testExecutionInBackgroundThread() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        executor.execute(latch::countDown);
        latch.await();
    }

    @Test(timeout = 10000)
    public void shouldIgnoreInterruptionOfWorkingThread() throws InterruptedException {
        executor.execute(() -> Thread.currentThread().interrupt());

        CountDownLatch latch = new CountDownLatch(1);
        executor.execute(latch::countDown);
        latch.await();
    }

    @Test(timeout = 10000)
    public void shouldAllowToStopMultipleTimes() throws InterruptedException {
        executor.stopExecutionThread();
        executor.stopExecutionThread();
    }

    @Test(timeout = 10000)
    public void shouldIgnoreExceptionsInWorkingThread() throws InterruptedException {
        executor.execute(() -> {throw new RuntimeException("test");});

        CountDownLatch latch = new CountDownLatch(1);
        executor.execute(latch::countDown);
        latch.await();
    }

    @Test(timeout = 10000)
    public void shouldExecuteTakInCurrentThreadAfterStopping() throws InterruptedException {
        executor.stopExecutionThread();

        AtomicReference<Thread> executionThread = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        executor.execute(() -> {
            executionThread.set(Thread.currentThread());
            latch.countDown();
        });
        latch.await();
        assertSame(Thread.currentThread(), executionThread.get());
    }

    @Test(timeout = 10000, expected = RuntimeException.class)
    public void shouldNotIgnoreExceptionsAfterStopping() throws InterruptedException {
        executor.stopExecutionThread();
        executor.execute(() -> {throw new RuntimeException("test");});
    }

}