/*
 *
 *  Copyright 2020 Vladimir Bukhtoyarov
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

package com.github.rollingmetrics.ranking.impl.util;

import com.github.rollingmetrics.ranking.Position;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class RankingTestData {

    public static long THRESHOLD_NANOS = TimeUnit.MICROSECONDS.toNanos(100);

    private static long latency = 1;
    public static Position first = new Position(latency, "first");
    public static Position second = new Position(latency + 1, "second");
    public static Position third = new Position(latency + 1, "third");
    public static Position fourth = new Position(latency + 2, "fourth");
    public static Position fifth = new Position(latency + 3, "fifth");
    public static Position sixth = new Position(latency + 3, "sixth");
    public static Position too_fast = new Position(THRESHOLD_NANOS - 1, "too_fast");

    public static String generateString(int length) {
        StringBuilder builder = new StringBuilder();
        IntStream.range(0, length).forEach((i) -> builder.append(" "));
        return builder.toString();
    }

}
