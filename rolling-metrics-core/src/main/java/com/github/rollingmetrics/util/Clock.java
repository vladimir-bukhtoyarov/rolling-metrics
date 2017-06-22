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
public interface Clock {

    /**
     * Returns the current time in milliseconds.
     *
     * @return the difference, measured in milliseconds, between the current time and midnight, January 1, 1970 UTC.
     */
    long currentTimeMillis();

    static Clock defaultClock() {
        return DEFAULT_CLOCK;
    }

    static Clock mock(AtomicLong currentTime) {
        return currentTime::get;
    }

    Clock DEFAULT_CLOCK = System::currentTimeMillis;

}
