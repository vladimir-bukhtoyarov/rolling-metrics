/*
 *
 *  Copyright 2017 Vladimir Bukhtoyarov
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

import java.util.concurrent.atomic.AtomicLong;

/**
 * The wrapper around time measurement which useful for unit testing purposes.
 */
public interface Ticker {

    /**
     * Returns the current value of the running Java Virtual Machine's high-resolution time source, in nanoseconds.
     *
     * @return the current value of the running Java Virtual Machine's high-resolution time source, in nanoseconds
     */
    long nanoTime();

    /**
     * Returns the current value of the running Java Virtual Machine's high-resolution time source, in milliseconds with following characteristics:
     * <ul>
     *     <li>Value is not sensitive to any change of system time.</li>
     *     <li>Value is always positive.</li>
     *     <li>Value is not correlated to {@link java.util.Date}.</li>
     * </ul>
     *
     * @return high-resolution time in milliseconds.
     */
    long stableMilliseconds();

    static Ticker defaultTicker() {
        return DefaultTicker.getInstance();
    }

    static Ticker mock(AtomicLong currentTimeMillis) {
        return new MockTicker(currentTimeMillis);
    }

}
