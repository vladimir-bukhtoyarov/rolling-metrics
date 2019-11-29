package com.github.rollingmetrics.micrometer;

import com.github.rollingmetrics.counter.SmoothlyDecayingRollingCounter;
import com.github.rollingmetrics.micrometer.meters.*;
import com.github.rollingmetrics.util.DaemonThreadFactory;
import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.instrument.distribution.pause.PauseDetector;
import io.micrometer.core.instrument.internal.DefaultGauge;
import io.micrometer.core.instrument.internal.DefaultLongTaskTimer;
import io.micrometer.core.instrument.internal.DefaultMeter;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.ToDoubleFunction;
import java.util.function.ToLongFunction;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RollingMeterRegistry extends MeterRegistry {
    private static final Logger logger = Logger.getLogger(RollingMeterRegistry.class.getName());

    private final DistributionStatisticConfig defaultConfig;
    private final TickerClock tickerClock;

    public RollingMeterRegistry(DistributionStatisticConfig defaultConfig) {
        this(defaultConfig, Clock.SYSTEM);
    }

    protected RollingMeterRegistry(DistributionStatisticConfig defaultConfig, Clock clock) {
        super(clock);
        this.defaultConfig = defaultConfig;
        this.tickerClock = new TickerClock(clock);

        if (defaultConfig.getBufferLength() == null) {
            throw new IllegalArgumentException("distributionStatisticConfig.getBufferLength() should not be null");
        }
        if (defaultConfig.getExpiry() == null) {
            throw new IllegalArgumentException("distributionStatisticConfig.getExpiry() should not be null");
        }

        Executors.newScheduledThreadPool(1, new DaemonThreadFactory("rolling-meter-function-updater")).scheduleAtFixedRate(
                () -> getMeters().forEach(meter -> {
                    try {
                        if (meter instanceof Updatable) {
                            ((Updatable) meter).update();
                        }
                    } catch (Throwable t) {
                        logger.log(Level.SEVERE, t, () -> "exception while updating meter " + meter.getId());
                    }
                }),
                0,
                defaultConfig.getExpiry().toMillis() / defaultConfig.getBufferLength(),
                TimeUnit.MILLISECONDS
        );
    }

    @Override
    protected <T> Gauge newGauge(Meter.Id id, T obj, ToDoubleFunction<T> valueFunction) {
        return new DefaultGauge<>(id, obj, valueFunction);
    }

    @Override
    protected Counter newCounter(Meter.Id id) {
        SmoothlyDecayingRollingCounter target = new SmoothlyDecayingRollingCounter(defaultConfig.getExpiry(), defaultConfig.getBufferLength(), tickerClock);
        return new Counter() {
            @Override
            public void increment(double amount) {
                target.add((long) amount);
            }

            @Override
            public double count() {
                return target.getSum();
            }

            @Override
            public Id getId() {
                return id;
            }
        };
    }

    @Override
    protected LongTaskTimer newLongTaskTimer(Meter.Id id) {
        return new DefaultLongTaskTimer(id, clock);
    }

    @Override
    protected Timer newTimer(Meter.Id id, DistributionStatisticConfig distributionStatisticConfig, PauseDetector pauseDetector) {
        return new RollingTimer(
                newDistributionSummary(id, distributionStatisticConfig, 1.0),
                clock
        );
    }

    @Override
    protected RollingDistributionSummary newDistributionSummary(Meter.Id id, DistributionStatisticConfig distributionStatisticConfig, double scale) {
        return new RollingDistributionSummary(id, distributionStatisticConfig, scale, clock);
    }

    @Override
    protected Meter newMeter(Meter.Id id, Meter.Type type, Iterable<Measurement> measurements) {
        return new DefaultMeter(id, type, measurements);
    }

    @Override
    protected <T> FunctionTimer newFunctionTimer(Meter.Id id, T obj, ToLongFunction<T> countFunction, ToDoubleFunction<T> totalTimeFunction, TimeUnit totalTimeFunctionUnit) {
        return new RollingFunctionTimer<>(id, obj, countFunction, totalTimeFunction, totalTimeFunctionUnit, defaultConfig, tickerClock);
    }

    @Override
    protected <T> FunctionCounter newFunctionCounter(Meter.Id id, T obj, ToDoubleFunction<T> countFunction) {
        return new RollingFunctionCounter<>(id, obj, countFunction, defaultConfig, tickerClock);
    }

    @Override
    protected TimeUnit getBaseTimeUnit() {
        return TimeUnit.MILLISECONDS;
    }

    @Override
    protected DistributionStatisticConfig defaultHistogramConfig() {
        return defaultConfig;
    }

}
