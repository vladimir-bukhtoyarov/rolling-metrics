package com.github.rollingmetrics.micrometer.meters;

import com.github.rollingmetrics.counter.SmoothlyDecayingRollingCounter;
import com.github.rollingmetrics.util.Ticker;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.ToDoubleFunction;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RollingFunctionCounter<T> implements FunctionCounter, Updatable {
    private static final Logger logger = Logger.getLogger(RollingFunctionCounter.class.getName());
    private final Id id;
    private final T obj;
    private final ToDoubleFunction<T> countFunction;
    private final AtomicReference<Double> lastCount = new AtomicReference<>(0.0);
    private final SmoothlyDecayingRollingCounter countCounter;

    public RollingFunctionCounter(Id id, T obj, ToDoubleFunction<T> countFunction, DistributionStatisticConfig config, Ticker tickerClock) {
        this.id = id;
        this.obj = obj;
        this.countFunction = countFunction;

        countCounter = new SmoothlyDecayingRollingCounter(config.getExpiry(), config.getBufferLength(), tickerClock);
    }

    @Override
    public double count() {
        update();
        return countCounter.getSum();
    }

    @Override
    public Id getId() {
        return id;
    }

    synchronized public void update() {
        double currentCount = countFunction.applyAsDouble(obj);

        if (currentCount < lastCount.get()) {
            logger.log(Level.FINE, () -> "count function for function counter " + id + " returned value " + currentCount + " that is less that previous value " + lastCount);
        } else {
            countCounter.add((long) (currentCount - lastCount.get()));
            lastCount.set(currentCount);
        }
    }
}
