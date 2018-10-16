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

/**
 * Implementation of ticker which based on {@link System#nanoTime()}.
 *
 * The {@link #stableMilliseconds()} method provides correct values at least for 192 years since creation.
 */
public class DefaultTicker implements Ticker {

    private static final Ticker INSTANCE = new DefaultTicker();

    private final long initializationNanoTime;


    DefaultTicker() {
        initializationNanoTime = nanoTime();
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
        return  (nanoTime() - initializationNanoTime) / 1_000_000;
    }

}
