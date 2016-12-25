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

package com.github.rollingmetrics.util;


import com.github.rollingmetrics.histogram.accumulator.ResetByChunksAccumulator;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Util class which should be used for execution tasks(like histogram rotation) in background.
 * Known clients: {@link ResetByChunksAccumulator}.
 *
 * For background execution this class maintains own implementation of executor {@link SingleThreadExecutor}
 * which extremely fast and has no blocking on task queueing.
 */
public final class ResilientExecutionUtil {

    private static final ThreadFactory DEFAULT_THREAD_FACTORY = new DaemonThreadFactory("metrics-core-hdr-background-worker");

    private static final ResilientExecutionUtil INSTANCE = new ResilientExecutionUtil();

    private volatile SingleThreadExecutor executorInstance;
    private ThreadFactory threadFactory = DEFAULT_THREAD_FACTORY;

    /**
     * @return instance of {@link ResilientExecutionUtil}
     */
    public static ResilientExecutionUtil getInstance() {
        return INSTANCE;
    }

    /**
     * The constructor visibility is package-private for unit testing
     */
    ResilientExecutionUtil() {
        // do nothing
    }

    /**
     * Executes task on executor. If executor is terminated or overloaded then executes task in current thread.
     *
     * @param executor
     * @param task
     */
    public void execute(Executor executor, Runnable task) {
        if (executor instanceof SingleThreadExecutor) {
            // we can trust to own executor implementation and schedule task without advanced control
            executor.execute(task);
            return;
        }

        // We are having deal with unknown executor implementation in the unknown state.
        // So we need to execute task in current thread if executor failed to do it by itself,
        // but to avoid of twice execution we need to wrap around the task by AtomicBoolean
        AtomicBoolean alreadyExecuted = new AtomicBoolean(false);
        Runnable oneShotRunnable = () -> {
            if (alreadyExecuted.compareAndSet(false, true)) {
                task.run();
            }
        };
        try {
            executor.execute(oneShotRunnable);
        } catch (Throwable e) {
            // The executor can be stopped or overloaded,
            // just execute task in current thread and ignore exception
            oneShotRunnable.run();
        }
    }

    /**
     * @return instance of {@link SingleThreadExecutor}
     */
    public Executor getBackgroundExecutor() {
        return getExecutorInstance();
    }

    /**
     * If the {@link #getBackgroundExecutor} was called before, the this method perform shutdown of background execution thread.
     */
    public synchronized void shutdownBackgroundExecutor() {
        if (executorInstance != null) {
            executorInstance.stopExecutionThread();
        }
    }

    /**
     * Sets the thread threadFactory which will be used for construction of {@link SingleThreadExecutor}.
     * <p>
     * This method should be called strongly before first invocation of {@link #getBackgroundExecutor}.
     * </p>
     *
     * <p>
     * This method is designed to be used in restricted environments with enabled SecurityManager,
     * when creation of new thread can fail by security limitation.
     * Normally, you should not use this method, because {@link #DEFAULT_THREAD_FACTORY} is quit enough.
     * </p>
     *
     * @param threadFactory
     * @throws IllegalStateException if executor already created
     */
    public synchronized void setThreadFactory(ThreadFactory threadFactory) {
        if (executorInstance != null) {
            String msg = "The executor instance already created with " + threadFactory +
                    ", so it is impossible to replace threadFactory." +
                    " You should call setThreadFactory strongly before first invocation of getBackgroundExecutor";
            throw new IllegalStateException(msg);
        }
        this.threadFactory = Objects.requireNonNull(threadFactory);
    }

    private SingleThreadExecutor getExecutorInstance() {
        SingleThreadExecutor executorInstance = this.executorInstance;
        if (executorInstance == null) {
            synchronized (this) {
                executorInstance = this.executorInstance;
                if (executorInstance == null) {
                    executorInstance = new SingleThreadExecutor(threadFactory);
                    this.executorInstance = executorInstance;
                }
            }
        }
        return executorInstance;
    }

}
