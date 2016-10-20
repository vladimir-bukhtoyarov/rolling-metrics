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

package com.github.metricscore.hdr.util;


import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.StampedLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BackgroundExecutionUtil implements Executor {

    private static final Runnable POISON = () -> {};
    private static final Runnable PARK = () -> {};

    private static final Logger logger = Logger.getLogger(BackgroundExecutionUtil.class.getName());
    private static ThreadFactory factory = createDefaultThreadFactory();
    private static ExecutionUtilHolder holder;

    private final StampedLock stampedLock = new StampedLock();
    private final ConcurrentLinkedQueue<Runnable> taskQueue = new ConcurrentLinkedQueue<>();
    private final Thread workerThread;

    /**
     * {@inheritDoc}
     *
     * TODO
     *
     * @param task
     */
    @Override
    public void execute(Runnable task) {
        long stamp = stampedLock.tryReadLock();
        if (stamp == 0) {
            task.run();
            return;
        }
        try {
            taskQueue.add(task);
            LockSupport.unpark(workerThread);
        } finally {
            stampedLock.unlockRead(stamp);
        }
    }

    /**
     *
     * TODO
     */
    public static void shutdownBackgroundExecutor() {
        getInstance().stop();
    }

    /**
     * TODO
     *
     * @param factory
     */
    synchronized public static void setThreadFactory(ThreadFactory factory) {
        BackgroundExecutionUtil.factory = Objects.requireNonNull(factory);
    }

    private BackgroundExecutionUtil() {
        this.workerThread = factory.newThread(lifecycle);
    }

    private void start() {
        workerThread.start();
    }

    private void stop() {
        if (stampedLock.isWriteLocked()) {
            // already stopped
            return;
        }

        // lock and never free in order to prevent future queueing
        stampedLock.writeLock();

        /*
         * Because of concurrent queue follow first-in first-out semantic,
          *we have guarantee that POISON will be a last task in the QUEUE.
         */
        taskQueue.add(POISON);
        LockSupport.unpark(workerThread);
    }

    public static Executor getBackgroundExecutor() {
        return getInstance();
    }

    public static BackgroundExecutionUtil getInstance() {
        if (holder == null) {
            synchronized (BackgroundExecutionUtil.class) {
                if (holder == null) {
                    BackgroundExecutionUtil instance = new BackgroundExecutionUtil();
                    holder = new ExecutionUtilHolder(instance);
                    instance.start();
                }
            }
        }
        return holder.instance;
    }

    private Runnable lifecycle = () -> {
        while (true) {
            Thread.interrupted();
            Runnable task = taskQueue.poll();
            if (task == POISON) {
                return;
            }
            if (task == PARK) {
                task = taskQueue.poll();
                if (task == null) {
                    LockSupport.park(BackgroundExecutionUtil.this);
                    continue;
                }
            }
            if (task != null) {
                executeAndLogErrors(task);
            } else {
                taskQueue.add(PARK);
            }
        }
    };

    private static void executeAndLogErrors(Runnable task) {
        try {
            task.run();
        } catch (Throwable e) {
            logger.log(Level.SEVERE, e, () -> "Fail to execute " + task + " in async mode because of " + e.getMessage());
        }
    }

    private static ThreadFactory createDefaultThreadFactory() {
        return runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("metrics-core-hdr-background-worker");
            thread.setDaemon(true);
            return thread;
        };
    }

    private static class ExecutionUtilHolder {

        private final BackgroundExecutionUtil instance;

        ExecutionUtilHolder(BackgroundExecutionUtil instance) {
            this.instance = instance;
        }
    }

}
