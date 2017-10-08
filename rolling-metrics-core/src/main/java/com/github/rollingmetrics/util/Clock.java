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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The wrapper around time measurement which useful for unit testing purposes.
 */
public interface Clock {

    /**
     * Returns the current time in milliseconds.
     *
     * @return the difference, measured in milliseconds, between the current time and midnight, January 1, 1970 UTC.
     */
    long currentTimeMillis();

    /**
     * Returns the current value of the running Java Virtual Machine's high-resolution time source, in nanoseconds.
     *
     * @return the current value of the running Java Virtual Machine's high-resolution time source, in nanoseconds
     */
    long nanoTime();

    static Clock defaultClock() {
        return DEFAULT_CLOCK;
    }

    static Clock mock(AtomicLong currentTime) {
        return new Clock() {
            @Override
            public long currentTimeMillis() {
                return currentTime.get();
            }

            @Override
            public long nanoTime() {
                return TimeUnit.MILLISECONDS.toNanos(currentTimeMillis());
            }
        };
    }

    Clock DEFAULT_CLOCK = new Clock() {

        @Override
        public long currentTimeMillis() {
            return System.currentTimeMillis();
        }

        @Override
        public long nanoTime() {
            return System.nanoTime();
        }
    };

}
