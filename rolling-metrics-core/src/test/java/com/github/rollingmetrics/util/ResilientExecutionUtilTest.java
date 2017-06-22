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
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class ResilientExecutionUtilTest {

    private ResilientExecutionUtil util = new ResilientExecutionUtil();

    @Test(expected = IllegalStateException.class)
    public void shouldDisallowReplaceThreadFactoryWhenExecutorAlreadyCreated() {
        util.getBackgroundExecutor();
        util.setThreadFactory(new DaemonThreadFactory(""));
    }

    @Test(timeout = 10000)
    public void shouldApplyThreadFactory() throws InterruptedException {
        ThreadFactory factory = new DaemonThreadFactory("42");
        util.setThreadFactory(factory);
        Executor executor = util.getBackgroundExecutor();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> threadName = new AtomicReference<>();
        executor.execute(() -> {
            threadName.set(Thread.currentThread().getName());
            latch.countDown();
        });

        latch.await();
        assertEquals("42", threadName.get());
    }

    @Test(timeout = 10000)
    public void shouldCorrectlyStopBackgroundExecutor() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Thread[] executorThread = new Thread[1];
        Runnable task = () -> {
            executorThread[0] = Thread.currentThread();
            latch.countDown();
        };
        Executor executor = util.getBackgroundExecutor();
        util.execute(executor, task);
        latch.await();
        util.shutdownBackgroundExecutor();
        executorThread[0].join();
    }

    @Test(timeout = 10000)
    public void shouldAlwaysReturnSameInstanceOfBackgroundExecutor() throws InterruptedException {
        assertSame(util.getBackgroundExecutor(), util.getBackgroundExecutor());
    }

    @Test(timeout = 10000)
    public void shutdownShouldNotLeadToThreadInitialization() throws InterruptedException {
        DaemonThreadFactory factory = new DaemonThreadFactory("42");
        util.setThreadFactory(factory);
        util.shutdownBackgroundExecutor();
        assertEquals(0, factory.getCreatedThreads());
    }

    @Test(timeout = 10000)
    public void shouldExecuteTaskInCurrentThreadWhenExecutorThrowException() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Executor executor = task -> {throw new RejectedExecutionException();};
        Runnable task = latch::countDown;
        util.execute(executor, task);

        latch.await();
    }

    @Test(timeout = 10000)
    public void shouldExecuteTaskInCurrentThreadExactlyOnceWhenExecutorThrowException() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger executionCount = new AtomicInteger();

        Executor executor = task -> {
            task.run();
            throw new RejectedExecutionException();
        };
        Runnable task = () -> {
            executionCount.incrementAndGet();
            latch.countDown();
        };
        util.execute(executor, task);

        latch.await();
        assertEquals(1, executionCount.get());
    }

}