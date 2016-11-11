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

package com.github.rollingmetrics.top;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class TestData {

    public static long MAX_DESCRIPTION_LENGH = 1000;
    public static long THRESHOLD_NANOS = TimeUnit.MICROSECONDS.toNanos(100);

    private static long timestamp = 1;
    private static long latency = 1;
    public static Position first = new Position(timestamp, latency, TimeUnit.MILLISECONDS, "first");
    public static Position second = new Position(timestamp, latency + 1, TimeUnit.MILLISECONDS, "second");
    public static Position third = new Position(timestamp + 1, latency + 1, TimeUnit.MILLISECONDS, "third");
    public static Position fourth = new Position(timestamp + 1, latency + 2, TimeUnit.MILLISECONDS, "fourth");
    public static Position fifth = new Position(timestamp + 1, latency + 3, TimeUnit.MILLISECONDS, "fifth");
    public static Position sixth = new Position(timestamp + 3, latency + 3, TimeUnit.MILLISECONDS, "sixth");
    public static Position too_fast = new Position(timestamp, THRESHOLD_NANOS - 1, TimeUnit.NANOSECONDS, "too_fast");

    public static String generateString(int length) {
        StringBuilder builder = new StringBuilder();
        IntStream.range(0, length).forEach((i) -> builder.append(" "));
        return builder.toString();
    }

}
