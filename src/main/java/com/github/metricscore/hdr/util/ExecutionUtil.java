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

public class ExecutionUtil {

    private static final Runnable POISON = () -> {};
    private static final Runnable PARK = () -> {};

    private static final Logger logger = Logger.getLogger(ExecutionUtil.class.getName());
    private static ThreadFactory factory = createDefaultThreadFactory();
    private static ExecutionUtilHolder holder;

    private final StampedLock stampedLock = new StampedLock();
    private ConcurrentLinkedQueue<Runnable> taskQueue = new ConcurrentLinkedQueue<>();
    private final Thread workerThread;

    private ExecutionUtil() {
        this.workerThread = factory.newThread(lifecycle);
    }

    private void start() {
        workerThread.start();
    }

    public static void shutdown() {
        getInstance().stop();
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

    synchronized public static void setThreadFactory(ThreadFactory factory) {
        ExecutionUtil.factory = Objects.requireNonNull(factory);
    }

    public static ExecutionUtil getInstance() {
        if (holder == null) {
            synchronized (ExecutionUtil.class) {
                if (holder == null) {
                    holder = new ExecutionUtilHolder(new ExecutionUtil());
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
                    LockSupport.park(ExecutionUtil.this);
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

        private final ExecutionUtil instance;

        public ExecutionUtilHolder(ExecutionUtil instance) {
            this.instance = instance;
        }
    }

}
