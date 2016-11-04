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

package com.github.metricscore.hdr.top;

import java.util.concurrent.TimeUnit;

public class TestData {

    private static long timestamp = 1;
    private static long latency = 1;

    public static Position first = new Position(timestamp, latency, TimeUnit.MILLISECONDS, "select version()");
    public static Position second = new Position(timestamp, latency + 1, TimeUnit.MILLISECONDS, "select version()");
    public static Position third = new Position(timestamp + 1, latency + 1, TimeUnit.MILLISECONDS, "select version()");
    public static Position fourth = new Position(timestamp + 1, latency + 2, TimeUnit.MILLISECONDS, "select version()");
    public static Position fifth = new Position(timestamp + 1, latency + 2, TimeUnit.MILLISECONDS, "select version()");

}
