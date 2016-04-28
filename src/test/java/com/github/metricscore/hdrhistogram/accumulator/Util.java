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

package com.github.metricscore.hdrhistogram.accumulator;

import com.codahale.metrics.Reservoir;

import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

public class Util {

    public static void runInParallel(Reservoir reservoir, Duration duration) throws InterruptedException {
        AtomicBoolean stopFlag = new AtomicBoolean(false);

        // let concurrent threads to work fo 3 seconds
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                stopFlag.set(true);
            }
        }, duration.toMillis());

        Thread[] threads = new Thread[Runtime.getRuntime().availableProcessors() * 2];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                try {
                    // update reservoir 100 times and take snapshot on each cycle
                    while (!stopFlag.get()) {
                        for (int j = 1; j <= 100; j++) {
                            reservoir.update(ThreadLocalRandom.current().nextInt(j));
                        }
                        reservoir.getSnapshot();
                    }
                } catch (Exception e){
                    e.printStackTrace();
                }
            });
            threads[i].start();
        }
        for (Thread thread: threads) {
            thread.join();
        }
    }
}
