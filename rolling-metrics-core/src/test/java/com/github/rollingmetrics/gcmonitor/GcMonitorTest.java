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

package com.github.rollingmetrics.gcmonitor;

import org.junit.Test;

import java.time.Duration;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;


public class GcMonitorTest {

    private MockTicker ticker = new MockTicker(0);

    private GarbageCollectorMXBeanMock young = new GarbageCollectorMXBeanMock("G1 Young Generation");
    private GarbageCollectorMXBeanMock old = new GarbageCollectorMXBeanMock("G1 Old Generation");

    GcMonitor monitor = GcMonitor.builder(Arrays.asList(young, old))
            .withTicker(ticker)
            .addRollingWindow("30sec", Duration.ofSeconds(30))
            .addRollingWindow("5min", Duration.ofMinutes(5))
            .build();

    @Test
    public void testNormalLifeCycle() {
        assertEquals(young.getListenersCount(), 0);
        assertEquals(old.getListenersCount(), 0);

        monitor.start();
        assertEquals(young.getListenersCount(), 1);
        assertEquals(old.getListenersCount(), 1);

        monitor.stop();
        assertEquals(young.getListenersCount(), 0);
        assertEquals(old.getListenersCount(), 0);
    }

    @Test
    public void startingTwiceShouldNotRaiseError() {
        monitor.start();
        monitor.start();

        assertEquals(young.getListenersCount(), 1);
        assertEquals(old.getListenersCount(), 1);
    }

    @Test
    public void startingAfterStoppingShouldBeSilentlyIgnored() {
        monitor.start();
        monitor.stop();
        monitor.start();

        assertEquals(young.getListenersCount(), 0);
        assertEquals(old.getListenersCount(), 0);
    }

    @Test
    public void stoppingShouldSilentlyIgnoredIfNotStarted() {
        monitor.stop();
        assertEquals(young.getListenersCount(), 0);
        assertEquals(old.getListenersCount(), 0);
    }

    @Test
    public void stoppingShouldSilentlyIgnoredIfAlreadyStopped() {
        monitor.start();
        monitor.stop();
        monitor.stop();

        assertEquals(young.getListenersCount(), 0);
        assertEquals(old.getListenersCount(), 0);
    }

    @Test
    public void testToString() {
        System.out.println(monitor.toString());

        monitor.start();
        System.out.println(monitor.toString());

        monitor.stop();
        System.out.println(monitor.toString());
    }

}