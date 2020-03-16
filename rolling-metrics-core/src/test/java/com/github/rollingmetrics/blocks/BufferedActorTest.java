/*
 *    Copyright 2020 Vladimir Bukhtoyarov
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

package com.github.rollingmetrics.blocks;

import org.junit.Test;
import org.openjdk.jol.info.ClassLayout;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static java.lang.System.out;
import static org.junit.Assert.*;

public class BufferedActorTest {

    @Test
    public void test() throws InterruptedException {
        int sharedSum[] = new int[1];

        BufferedActor[] actorRef = new BufferedActor[1];

        Supplier<BufferedActor.ReusableActionContainer> actionFactory = () -> new BufferedActor.ReusableActionContainer() {
            @Override
            public void freeGarbage() {
            }

            @Override
            public void run() {
                sharedSum[0] = sharedSum[0] + 1;

//                if (sharedSum[0]%1_000 == 0) {
//                    System.out.println(sharedSum[0] + ":" + Thread.currentThread().getName() + " action pool size=" + actorRef[0].getActionPoolSize() + " in progress action count " + actorRef[0].getScheduledActionsCount() + " overflow count " + actorRef[0].getBlockedCount());
//                }
            }
        };

        BufferedActor<BufferedActor.ReusableActionContainer> actor = new BufferedActor<>(actionFactory, 512, 1024);
        actorRef[0] = actor;

        int iterationsPerThread = 10_000_000;
        Thread[] threads = new Thread[4];
        CountDownLatch latch = new CountDownLatch(threads.length);
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                latch.countDown();
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                for (int j = 0; j < iterationsPerThread; j++) {
                    try {
                        BufferedActor.ReusableActionContainer action = actor.getActionFromPool();
                        actor.doExclusivelyOrSchedule(action);
                        if (j % 1000 == 0) {
                            try {
                                TimeUnit.NANOSECONDS.sleep(1);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (RuntimeException e) {
                        throw new RuntimeException("Error happen on step " + j + " - " + e.getMessage(), e);
                    }
                }
            });
            threads[i].setName("Incrementor-" + (i+1));
            threads[i].start();
        }

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println(sharedSum[0] + ":" + " blocked count " + actorRef[0].getOverflowedCount());
            }
        }, 1, 1000);

        for (int i = 0; i < threads.length; i++) {
            threads[i].join();
        }
        timer.cancel();

        System.out.println("result sum:" + sharedSum[0]);
        System.out.println("blocked count:" + actor.getOverflowedCount());

        assertEquals(sharedSum[0], iterationsPerThread * threads.length);
    }

    public static void main(String[] args) {
        out.println(ClassLayout.parseClass(BufferedActor.class).toPrintable());
    }

}