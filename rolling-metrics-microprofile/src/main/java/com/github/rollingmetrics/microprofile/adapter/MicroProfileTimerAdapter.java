/*
 *    Copyright 2017 Vladimir Bukhtoyarov
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

package com.github.rollingmetrics.microprofile.adapter;

import com.github.rollingmetrics.histogram.hdr.RollingHdrHistogram;
import com.github.rollingmetrics.util.Clock;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.Snapshot;
import org.eclipse.microprofile.metrics.Timer;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Adapter from rolling-hdr histogram to Eclipse-MicroProfile timer
 */
public class MicroProfileTimerAdapter implements Timer {

    private final Histogram histogram;
    private final Meter meter;
    private final Clock clock;

    public MicroProfileTimerAdapter(RollingHdrHistogram rollingHistogram, Meter meter) {
        this(rollingHistogram, meter, Clock.defaultClock());
    }

    public MicroProfileTimerAdapter(RollingHdrHistogram rollingHistogram, Meter meter, Clock clock) {
        this.histogram = new MicroProfileHistogramAdapter(rollingHistogram);
        this.meter = Objects.requireNonNull(meter);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public void update(long duration, TimeUnit unit) {
        update(unit.toNanos(duration));
    }

    @Override
    public <T> T time(Callable<T> callable) throws Exception {
        final long startTime = clock.nanoTime();
        try {
            return callable.call();
        } finally {
            update(clock.nanoTime() - startTime);
        }
    }

    @Override
    public void time(Runnable runnable) {
        final long startTime = clock.nanoTime();
        try {
            runnable.run();
        } finally {
            update(clock.nanoTime() - startTime);
        }
    }

    @Override
    public Context time() {
        final long startTimeNanos = clock.currentTimeMillis();
        return new Context() {
            @Override
            public long stop() {
                final long elapsedNanos = clock.currentTimeMillis() - startTimeNanos;
                update(elapsedNanos);
                return elapsedNanos;
            }

            @Override
            public void close() {
                stop();
            }
        };
    }

    @Override
    public long getCount() {
        return histogram.getCount();
    }

    @Override
    public double getFifteenMinuteRate() {
        return meter.getFifteenMinuteRate();
    }

    @Override
    public double getFiveMinuteRate() {
        return meter.getFiveMinuteRate();
    }

    @Override
    public double getMeanRate() {
        return meter.getMeanRate();
    }

    @Override
    public double getOneMinuteRate() {
        return meter.getOneMinuteRate();
    }

    @Override
    public Snapshot getSnapshot() {
        return histogram.getSnapshot();
    }

    private void update(long durationNanos) {
        if (durationNanos >= 0) {
            histogram.update(durationNanos);
            meter.mark();
        }
    }

}
