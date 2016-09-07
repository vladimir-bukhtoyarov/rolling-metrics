/*
 *    Copyright 2016 Vladimir Bukhtoyarov
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package examples;

import com.github.metricscore.hdr.counter.SmoothlyDecayingRollingCounter;
import com.github.metricscore.hdr.counter.WindowCounter;

import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class ResetByChunkCounterExample {

    public static void main(String[] args) {
        final WindowCounter counter = new SmoothlyDecayingRollingCounter(Duration.ofSeconds(1), 7);

        // report sum each second
        System.out.println("Waiting 9 seconds before start sum reporting");
        AtomicLong previousSumRef = new AtomicLong(Long.MIN_VALUE);
        new Timer("report-sum", true).scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                long sum = counter.getSum();
                long previousSum = previousSumRef.get();
                double difPercentage = 0.0;
                if (previousSum != Long.MIN_VALUE) {
                    double diff = Math.abs(previousSum - sum);
                    difPercentage = diff / previousSum * 100;
                }
                System.out.println("sum = " + sum + " ; diff: " + difPercentage + "%");
                previousSumRef.set(sum);
            }
        }, 9000, 1000);

        // stop test after five minutes
        AtomicBoolean stopped = new AtomicBoolean();
        new Timer("test-finalizer", true).schedule(new TimerTask() {
            @Override
            public void run() {
                stopped.set(true);
            }
        }, TimeUnit.MINUTES.toMillis(5));

        while (!stopped.get()) {
            counter.add(1);
        }
    }

}
