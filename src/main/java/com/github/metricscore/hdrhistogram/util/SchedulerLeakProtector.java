package com.github.metricscore.hdrhistogram.util;


import java.lang.ref.WeakReference;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class SchedulerLeakProtector {

    public static <T> ScheduledFuture<?> scheduleAtFixedRate(ScheduledExecutorService scheduler, T target, Consumer<T> consumer, long initialDelay, long period, TimeUnit timeUnit) {
        CompletableFuture<ScheduledFuture<?>> scheduledFutureFuture = new CompletableFuture<>();
        LeakProtectedRunnable<T> runnable = new LeakProtectedRunnable<>(target, consumer, scheduledFutureFuture);
        ScheduledFuture<?> scheduledFuture = scheduler.scheduleAtFixedRate(runnable, initialDelay, period, timeUnit);
        scheduledFutureFuture.complete(scheduledFuture);
        return scheduledFuture;
    }

    private static final class LeakProtectedRunnable<T> implements Runnable {

        private final WeakReference<T> targetReference;
        private final Consumer<T> consumer;
        private final CompletableFuture<ScheduledFuture<?>> scheduledFutureFuture;

        LeakProtectedRunnable(T target, Consumer<T> consumer, CompletableFuture<ScheduledFuture<?>> scheduledFutureFuture) {
            this.targetReference = new WeakReference<>(target);
            this.consumer = consumer;
            this.scheduledFutureFuture = scheduledFutureFuture;
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
            try {
                scheduledFutureFuture.get().cancel(false);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

    }

}
