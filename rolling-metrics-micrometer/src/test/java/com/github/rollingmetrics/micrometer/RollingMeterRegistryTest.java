package com.github.rollingmetrics.micrometer;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.instrument.distribution.HistogramSnapshot;
import org.junit.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class RollingMeterRegistryTest {

    private final RollingMeterRegistry registry;
    private final MockClock clock;

    public RollingMeterRegistryTest() {
        DistributionStatisticConfig config = DistributionStatisticConfig.builder()
                .expiry(Duration.ofSeconds(10))
                .bufferLength(3)
                .build();
        clock = new MockClock();
        registry = new RollingMeterRegistry(config, clock);
    }

    @Test
    public void newGauge() {
        AtomicInteger value = new AtomicInteger(42);
        registry.gauge("my-gauge", Collections.emptyList(), value, AtomicInteger::intValue);

        Gauge gauge = getMeter("my-gauge");
        assertEquals(42, gauge.value(), 0.0001);
    }

    @Test
    public void newCounter() {
        Counter counter = registry.counter("my-counter");
        counter.increment();
        counter.increment();
        counter.increment();
        assertEquals(3, counter.count(), 0.0001);
    }

    @Test
    public void newLongTaskTimer() {
        LongTaskTimer timer = LongTaskTimer.builder("my-long-timer").register(registry);
        timer.start();
        timer.start();
        assertEquals(2, timer.activeTasks());
    }

    @Test
    public void newTimer() {
        Timer timer = registry.timer("my-timer");
        timer.record(() -> {
            clock.add(Duration.ofMillis(1000));
        });
        timer.record(() -> {
            clock.add(Duration.ofMillis(100));
        });
        timer.record(() -> {
            clock.add(Duration.ofMillis(500));
        });

        HistogramSnapshot snapshot = timer.takeSnapshot();
        assertEquals(3, snapshot.count());
        assertEquals(1000, snapshot.max(), 50);
        assertEquals((1000 + 100 + 500) / 3.0, snapshot.mean(), 50);
        assertEquals(500, snapshot.percentileValues()[0].value(), 50);
    }

    @Test
    public void newDistributionSummary() {
        DistributionSummary histogram = registry.summary("histogram");
        histogram.record(1000);
        histogram.record(500);
        histogram.record(100);
        HistogramSnapshot snapshot = histogram.takeSnapshot();
        assertEquals(3, snapshot.count());
        assertEquals(1000, snapshot.max(), 50);
        assertEquals((1000 + 100 + 500) / 3.0, snapshot.mean(), 50);
        assertEquals(500, snapshot.percentileValues()[0].value(), 50);
    }

    @Test
    public void newDistributionSummary_empty() {
        DistributionSummary histogram = registry.summary("histogram");
        HistogramSnapshot snapshot = histogram.takeSnapshot();
        assertEquals(0, snapshot.count());
        assertEquals(0, snapshot.max(), 0);
        assertEquals(0, snapshot.mean(), 0);
        assertEquals(0, snapshot.percentileValues()[0].value(), 0);
    }

    @Test
    public void newFunctionTimer() {
        AtomicLong counter = new AtomicLong(42);
        AtomicReference<Double> time = new AtomicReference<>(666.0);

        FunctionTimer timer = FunctionTimer.builder("timer", new Object(), value -> counter.get(), value -> time.get(), TimeUnit.MINUTES).register(registry);
        assertEquals(42, timer.count(), 0.001);
        assertEquals(60*666_000, timer.totalTime(TimeUnit.MILLISECONDS), 0.001);
    }

    @Test
    public void newFunctionCounter() {
        AtomicLong counter = new AtomicLong(42);
        FunctionCounter counterr = FunctionCounter.builder("counter", new Object(), value -> counter.get()).register(registry);
        assertEquals(42, counterr.count(), 0.001);
    }

    public <T extends Meter> T getMeter(String name) {
        return (T) registry.getMeters().stream()
                .filter(meter -> meter.getId().getName().equals(name))
                .findAny()
                .get();
    }
}