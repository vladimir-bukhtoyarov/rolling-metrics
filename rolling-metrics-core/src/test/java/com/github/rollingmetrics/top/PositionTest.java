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

package com.github.rollingmetrics.top;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class PositionTest {

    @Test(expected = IllegalArgumentException.class)
    public void shouldDisallowNullDescription() {
        new Position(System.currentTimeMillis(), 22, TimeUnit.MILLISECONDS, null, 23);
    }

    @Test
    public void shouldCorrectlyConverLatencyToNanoseconds() {
        Position position = new Position(System.currentTimeMillis(), 2, TimeUnit.MILLISECONDS, "SELECT * FROM DUAL", 1000);
        assertEquals(2_000_000L, position.getLatencyInNanoseconds());
    }

    @Test
    public void shouldCorrectlyFormatDescription() {
        Position position = new Position(System.currentTimeMillis(), 2, TimeUnit.MILLISECONDS, "SELECT * FROM DUAL", 1000);
        assertEquals("SELECT * FROM DUAL", position.getQueryDescription());
    }

    @Test
    public void testToString() {
        Position position = new Position(System.currentTimeMillis(), 2, TimeUnit.MILLISECONDS, "SELECT * FROM DUAL", 1000);
        System.out.println(position.toString());
    }

}