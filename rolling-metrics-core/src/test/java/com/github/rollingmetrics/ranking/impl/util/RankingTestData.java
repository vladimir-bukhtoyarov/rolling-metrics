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

public class RankingTestData {

    public static long THRESHOLD_NANOS = TimeUnit.MICROSECONDS.toNanos(100);

    private static long latency = 0;
    public static Position first = new Position(latency + 1, "first");
    public static Position second = new Position(latency + 2, "second");
    public static Position third = new Position(latency + 3, "third");
    public static Position third_2 = new Position(latency + 3, "third-2");
    public static Position fourth = new Position(latency + 4, "fourth");
    public static Position fifth = new Position(latency + 5, "fifth");
    public static Position sixth = new Position(latency + 6, "sixth");
    public static Position too_fast = new Position(THRESHOLD_NANOS - 1, "too_fast");

}
