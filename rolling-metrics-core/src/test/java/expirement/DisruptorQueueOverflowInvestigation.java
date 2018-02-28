/*
 *
 *  Copyright 2018 Vladimir Bukhtoyarov
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

package expirement;

import com.github.rollingmetrics.util.DaemonThreadFactory;
import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

import java.util.concurrent.locks.LockSupport;

public class DisruptorQueueOverflowInvestigation {

    public static void main(String[] args) {
        EventFactory<MyEvent> factory = MyEvent::new;
        WaitStrategy waitStrategy = new SleepingWaitStrategy(1, 100_000_000);
        DaemonThreadFactory threadFactory = new DaemonThreadFactory("disruptor-thread");
        int bufferSize = 128;

        Disruptor<MyEvent> disruptor = new Disruptor<>(factory, bufferSize, threadFactory, ProducerType.MULTI, waitStrategy);

        EventHandler<? super MyEvent> eventHandler = new MyEventHandler();
        disruptor.handleEventsWith(eventHandler);

        disruptor.start();

        RingBuffer<MyEvent> ringBuffer = disruptor.getRingBuffer();
        for (int i = 0; i < 1000; i++) {
            long sequence;
            try {
                sequence = ringBuffer.tryNext();
            } catch (InsufficientCapacityException e) {
                e.printStackTrace();
                return;
            }
            try {
                MyEvent event = ringBuffer.get(sequence);
                event.value = i;
            } finally {
                ringBuffer.publish(sequence);
                System.out.println("Published " + i);
            }
        }
    }

    private static final class MyEvent {

        public MyEvent() {
            System.out.println("MyEvent has been created");
        }

        long value;

    }

    private static final class MyEventHandler implements EventHandler<MyEvent> {

        @Override
        public void onEvent(MyEvent event, long sequence, boolean endOfBatch) throws Exception {
            System.out.println("got " + event.value);
            LockSupport.park();
        }

    }

}
