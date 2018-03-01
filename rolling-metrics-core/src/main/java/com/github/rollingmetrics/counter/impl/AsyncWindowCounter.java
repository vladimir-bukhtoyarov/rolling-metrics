/*
 *    Copyright 2018 Vladimir Bukhtoyarov
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

package com.github.rollingmetrics.counter.impl;

import com.github.rollingmetrics.counter.WindowCounter;
import com.github.rollingmetrics.retention.disruptor.AsyncMetric;
import com.github.rollingmetrics.retention.disruptor.MonitoringDisruptor;
import com.github.rollingmetrics.retention.disruptor.MonitoringEvent;
import com.lmax.disruptor.InsufficientCapacityException;
import com.lmax.disruptor.RingBuffer;

import java.util.concurrent.atomic.AtomicLong;

public class AsyncWindowCounter implements WindowCounter, AsyncMetric {

    private final MonitoringDisruptor monitoringDisruptor;
    private final RingBuffer<MonitoringEvent> ringBuffer;

    private final AtomicLong sum = new AtomicLong();

    public AsyncWindowCounter(MonitoringDisruptor monitoringDisruptor) {
        this.monitoringDisruptor = monitoringDisruptor;
        this.ringBuffer = monitoringDisruptor.getRingBuffer();
    }

    @Override
    public void add(long delta) {
        long sequence;
        try {
            sequence = ringBuffer.tryNext();
        } catch (InsufficientCapacityException e) {
            monitoringDisruptor.registerOverflow();
            return;
        }
        try {
            MonitoringEvent event = ringBuffer.get(sequence);
            event.arg1 = delta;
            event.metric = this;
        } finally {
            ringBuffer.publish(sequence);
        }
    }

    @Override
    public long getSum() {
        return sum.get();
    }

    @Override
    public void apply(MonitoringEvent event) {
        sum.addAndGet(event.arg1);
    }

    @Override
    public void revert(MonitoringEvent event) {
        sum.addAndGet(-event.arg2);
    }

}
