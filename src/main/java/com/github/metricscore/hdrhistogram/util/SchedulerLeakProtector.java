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


import java.lang.ref.WeakReference;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Provides way to protect from leaking of strong reference through ScheduledExecutorService.
 */
public class SchedulerLeakProtector {

    public static <T> ScheduledFuture<?> scheduleAtFixedRate(ScheduledExecutorService scheduler, T target, Consumer<T> consumer, long initialDelay, long period, TimeUnit timeUnit) {
        return scheduleAtFixedRate(scheduler, new WeakReference<>(target), consumer, initialDelay, period, timeUnit);
    }

    public static <T> ScheduledFuture<?> scheduleAtFixedRate(ScheduledExecutorService scheduler, WeakReference<T> target, Consumer<T> consumer, long initialDelay, long period, TimeUnit timeUnit) {
        AtomicReference<ScheduledFuture<?>> scheduledFutureRef = new AtomicReference<>();
        LeakProtectedRunnable<T> runnable = new LeakProtectedRunnable<>(target, consumer, scheduledFutureRef);
        ScheduledFuture<?> scheduledFuture = scheduler.scheduleAtFixedRate(runnable, initialDelay, period, timeUnit);
        scheduledFutureRef.set(scheduledFuture);
        return scheduledFuture;
    }

    private static final class LeakProtectedRunnable<T> implements Runnable {

        private final WeakReference<T> targetReference;
        private final Consumer<T> consumer;
        private final AtomicReference<ScheduledFuture<?>> scheduledFutureRef;

        LeakProtectedRunnable(WeakReference<T> targetReference, Consumer<T> consumer, AtomicReference<ScheduledFuture<?>> scheduledFutureRef) {
            this.targetReference = targetReference;
            this.consumer = consumer;
            this.scheduledFutureRef = scheduledFutureRef;
        }

        @Override
        public void run() {
            T target = targetReference.get();
            if (target != null) {
                // target object still strong referenced
                consumer.accept(target);
                return;
            }

            // target object became unreachable lets cancel scheduled task
            scheduledFutureRef.get().cancel(false);
        }

    }

}
