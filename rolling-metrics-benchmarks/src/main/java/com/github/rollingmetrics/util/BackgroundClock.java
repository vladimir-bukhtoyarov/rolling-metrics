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

public class BackgroundClock implements Clock {

    private volatile long cachedTimeMillis;
    private final long precisionMillis;
    private final Thread thread;

    public BackgroundClock(long precisionMillis) {
        this.precisionMillis = precisionMillis;
        cachedTimeMillis = System.currentTimeMillis();
        thread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(precisionMillis);
                    cachedTimeMillis = System.currentTimeMillis();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return;
                }
            }
        });
        thread.setName("Background-clock");
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public long currentTimeMillis() {
        return cachedTimeMillis;
    }

    public void stop() {
        thread.interrupt();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
