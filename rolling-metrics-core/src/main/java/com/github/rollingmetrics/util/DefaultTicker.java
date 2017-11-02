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

package com.github.rollingmetrics.util;

import java.util.concurrent.TimeUnit;

/**
 * Implementation of ticker which based on {@link System#nanoTime()}.
 *
 * The {@link #stableMilliseconds()} method provides correct values at least for 192 years since creation.
 */
public class DefaultTicker implements Ticker {

    static final long BORDER_ZONE = TimeUnit.MINUTES.toNanos(1);
    static final long NANOS_IN_ONE_MILLIS = 1_000_000L;
    static final long MAX_MILLIS = Long.MAX_VALUE / NANOS_IN_ONE_MILLIS;

    private static final Ticker INSTANCE = new DefaultTicker();

    private final long positiveShiftMillis;
    private final long negativeShiftMillis;
    private final long sourceShiftNanos;

    DefaultTicker() {
        long nanotime = nanoTime();
        boolean nearToBorder = Long.MAX_VALUE - Math.abs(nanotime) <= BORDER_ZONE || Math.abs(nanotime) <= BORDER_ZONE;
        this.sourceShiftNanos = nearToBorder? BORDER_ZONE * 2 : 0;
        nanotime += sourceShiftNanos;
        if (nanotime >= 0) {
            positiveShiftMillis = 0;
            negativeShiftMillis = MAX_MILLIS * 2;
        } else {
            positiveShiftMillis = MAX_MILLIS;
            negativeShiftMillis = MAX_MILLIS;
        }
    }

    /**
     * @return the cached instance of {@link DefaultTicker}
     */
    public static Ticker getInstance() {
        return INSTANCE;
    }

    @Override
    public long nanoTime() {
        return System.nanoTime();
    }

    @Override
    public long stableMilliseconds() {
        long nanotime = nanoTime() + sourceShiftNanos;
        if (nanotime >= 0) {
            return nanotime / NANOS_IN_ONE_MILLIS + positiveShiftMillis;
        } else {
            return nanotime / NANOS_IN_ONE_MILLIS + negativeShiftMillis;
        }
    }

}
