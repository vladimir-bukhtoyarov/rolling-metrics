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

package examples;

import com.github.rollingmetrics.counter.WindowCounter;
import com.github.rollingmetrics.retention.RetentionPolicy;

import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class SmoothlyDecayingRollingCounterPrecisionDemo {

    public static void main(String[] args) throws InterruptedException {
        // the counter which storing measurements for last 10 seconds and split counter by 4 chunks
        final WindowCounter counter = RetentionPolicy
                .resetPeriodicallyByChunks(Duration.ofSeconds(10), 4)
                .newCounter();

        // report sum each second
        AtomicLong previousSumRef = new AtomicLong(Long.MIN_VALUE);
        new Timer("report-sum", true).scheduleAtFixedRate(new TimerTask() {

            AtomicInteger measureIndex = new AtomicInteger();

            @Override
            public void run() {
                long sum = counter.getSum();
                long previousSum = previousSumRef.get();
                long diff = Math.abs(previousSum - sum);
                double difPercentage = 0.0;
                if (previousSum != Long.MIN_VALUE) {
                    difPercentage = (double)diff / (double)previousSum * 100;
                }
                String message = String.format("%3d sum = %14d; dif = %10d; difPercentage = ", measureIndex.incrementAndGet(),  sum, diff);
                System.out.println(message + difPercentage + "%");
                previousSumRef.set(sum);
            }
        }, 0, 1000);

        // stop test after five minutes
        AtomicBoolean incrementingStopped = new AtomicBoolean();
        new Timer("test-finalizer", true).schedule(new TimerTask() {
            @Override
            public void run() {
                incrementingStopped.set(true);
            }
        }, TimeUnit.MINUTES.toMillis(1));

        while (!incrementingStopped.get()) {
            counter.add(1);
        }

        Thread.sleep(9000);
    }

}
