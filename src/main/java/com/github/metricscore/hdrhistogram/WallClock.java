/*
 *
 *  Copyright 2016 Vladimir Bukhtoyarov
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

package com.github.metricscore.hdrhistogram;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Wrapper around time measuring useful in unit tests to avoid sleeping.
 */
class WallClock {

    public static final WallClock INSTANCE = new WallClock();

    private WallClock() {}

    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    public static WallClock mock(AtomicLong currentTimeProvider) {
        return new WallClock() {
            @Override
            public long currentTimeMillis() {
                return currentTimeProvider.get();
            }
        };
    }

}
