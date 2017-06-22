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

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.StampedLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Executor implementation which normally executes tasks in dedicated thread.
 * This executor never throws RejectedExecutionException, because after it stopped by client,
 * it begin executes tasks in current caller thread.
 * This behavior provides graceful shutdown in complex applications when metrics can still be collected during shutdown.
 */
public class SingleThreadExecutor implements Executor {

    private static final Runnable POISON = () -> {};
    private static final Runnable PARK = () -> {};

    private static final Logger logger = Logger.getLogger(SingleThreadExecutor.class.getName());

    private final StampedLock stampedLock = new StampedLock();
    private final ConcurrentLinkedQueue<Runnable> taskQueue = new ConcurrentLinkedQueue<>();
    private final Thread workerThread;

    SingleThreadExecutor(ThreadFactory factory) {
        this.workerThread = factory.newThread(this::doLifeCycle);

        // Leaking reference to "SingleOrSameThreadExecutor.this" from constructor though lambda does not lead to publication problem,
        // because of "Thread#start" has HB relation with first instruction in new thread,
        // so all private fields will be visible as fully initialized inside "workerThread"
        workerThread.start();
    }

    /**
     * {@inheritDoc}
     *
     * If background thread is started then executes task in this thread, otherwise executes task in current thread.
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
     * Stops the executor thread
     */
    public void stopExecutionThread() {
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

    private void doLifeCycle() {
        while (true) {
            Thread.interrupted();
            Runnable task = taskQueue.poll();
            if (task == POISON) {
                return;
            }
            if (task == PARK) {
                task = taskQueue.poll();
                if (task == null) {
                    LockSupport.park(SingleThreadExecutor.this);
                    continue;
                }
            }
            if (task != null) {
                executeAndLogErrors(task);
            } else {
                taskQueue.add(PARK);
            }
        }
    }

    private static void executeAndLogErrors(Runnable task) {
        try {
            task.run();
        } catch (Throwable e) {
            logger.log(Level.SEVERE, e, () -> "Fail to execute " + task + " in async mode because of " + e.getMessage());
        }
    }

}
