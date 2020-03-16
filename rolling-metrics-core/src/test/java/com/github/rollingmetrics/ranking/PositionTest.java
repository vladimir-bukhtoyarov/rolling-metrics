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

package com.github.rollingmetrics.ranking;

import org.junit.Test;

public class PositionTest {

    @Test(expected = NullPointerException.class)
    public void shouldDisallowNullDescription() {
        new Position(22,null);
    }

    @Test
    public void testToString() {
        Position position = new Position(2, "SELECT * FROM DUAL");
        System.out.println(position.toString());
    }

}