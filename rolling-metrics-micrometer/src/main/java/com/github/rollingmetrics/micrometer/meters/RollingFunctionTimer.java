package com.github.rollingmetrics.micrometer.meters;

import com.github.rollingmetrics.counter.SmoothlyDecayingRollingCounter;
import com.github.rollingmetrics.util.Ticker;
import io.micrometer.core.instrument.FunctionTimer;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.instrument.util.TimeUtils;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.ToDoubleFunction;
import java.util.function.ToLongFunction;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RollingFunctionTimer<T> implements FunctionTimer, Updatable {
    private static final Logger logger = Logger.getLogger(RollingFunctionTimer.class.getName());
    private final Id id;
    private final T obj;
    private final ToLongFunction<T> countFunction;
    private final ToDoubleFunction<T> totalTimeFunction;
    private final TimeUnit totalTimeFunctionUnit;
    private final AtomicReference<Long> lastCount = new AtomicReference<>(0L);
    private final AtomicReference<Double> lastTime = new AtomicReference<>(0.0);
    private final SmoothlyDecayingRollingCounter countCounter;
    private final SmoothlyDecayingRollingCounter timeCounter;

    public RollingFunctionTimer(Id id, T obj, ToLongFunction<T> countFunction, ToDoubleFunction<T> totalTimeFunction, TimeUnit totalTimeFunctionUnit, DistributionStatisticConfig config, Ticker tickerClock) {
        this.id = id;
        this.obj = obj;
        this.countFunction = countFunction;
        this.totalTimeFunction = totalTimeFunction;
        this.totalTimeFunctionUnit = totalTimeFunctionUnit;

        countCounter = new SmoothlyDecayingRollingCounter(config.getExpiry(), config.getBufferLength(), tickerClock);
        timeCounter = new SmoothlyDecayingRollingCounter(config.getExpiry(), config.getBufferLength(), tickerClock);
    }

    @Override
    public double count() {
        update();
        return countCounter.getSum();
    }

    @Override
    public double totalTime(TimeUnit unit) {
        update();
        return timeCounter.getSum();
    }

    @Override
    public TimeUnit baseTimeUnit() {
        return TimeUnit.MILLISECONDS;
    }

    @Override
    public Id getId() {
        return id;
    }

    synchronized public void update() {
        long currentCount = countFunction.applyAsLong(obj);

        if (currentCount < lastCount.get()) {
            logger.log(Level.FINE, () -> "count function for function timer " + id + " returned value " + currentCount + " that is less that previous value " + lastCount);
        } else {
            countCounter.add(currentCount - lastCount.get());
            lastCount.set(currentCount);
        }

        double currentTime = TimeUtils.convert(totalTimeFunction.applyAsDouble(obj), totalTimeFunctionUnit, TimeUnit.MILLISECONDS);

        if (currentTime < lastTime.get()) {
            logger.log(Level.FINE, () -> "time function for function timer " + id + " returned value " + currentTime + " that is less that previous value " + lastTime);
        } else {
            timeCounter.add((long)(currentTime - lastTime.get()));
            lastTime.set(currentTime);
        }
    }
}
