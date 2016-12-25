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

package com.github.rollingmetrics.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class DaemonThreadFactory implements ThreadFactory {

    private final AtomicInteger createdThreads = new AtomicInteger(0);
    private final String threadNameFormat;

    public DaemonThreadFactory(String threadNameFormat) {
        this.threadNameFormat = threadNameFormat;
    }

    @Override
    public Thread newThread(Runnable runnable) {
        int threadNumber = this.createdThreads.incrementAndGet();
        String threadName = String.format(threadNameFormat, threadNumber);
        Thread thread = new Thread(runnable);
        thread.setName(threadName);
        thread.setDaemon(true);
        return thread;
    }

    @Override
    public String toString() {
        return "DaemonThreadFactory{" +
                "createdThreads=" + createdThreads +
                ", threadNameFormat='" + threadNameFormat + '\'' +
                '}';
    }

    public int getCreatedThreads() {
        return createdThreads.get();
    }

}
