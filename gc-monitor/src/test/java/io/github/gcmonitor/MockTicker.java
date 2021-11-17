/*
 *  Copyright 2017 Vladimir Bukhtoyarov
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.github.gcmonitor;


import com.github.rollingmetrics.util.Ticker;

public class MockTicker implements Ticker {

    private long currentTimeMillis;

    public MockTicker(long currentTimeMillis) {
        this.currentTimeMillis = currentTimeMillis;
    }

    public void setCurrentTimeMillis(long currentTimeMillis) {
        this.currentTimeMillis = currentTimeMillis;
    }

    public void moveForward(long timeIncrementMillis) {
        currentTimeMillis += timeIncrementMillis;
    }

    @Override
    public long nanoTime() {
        return currentTimeMillis * 1_000_000;
    }

    @Override
    public long stableMilliseconds() {
        return currentTimeMillis;
    }
}
