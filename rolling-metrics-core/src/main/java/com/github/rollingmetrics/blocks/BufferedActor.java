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

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;


class BufferedActor_ConcurrentFields<T extends BufferedActor.ReusableActionContainer> {

    final AtomicIntegerArray poolLocks;
    final AtomicReference<BufferedActor.ReusableActionContainer> scheduledActionsStackTop;
    final ReentrantLock lock = new ReentrantLock();

    BufferedActor_ConcurrentFields(Supplier<T> actionFactory, int bufferSize) {
        scheduledActionsStackTop = new AtomicReference<>();
        this.poolLocks = new AtomicIntegerArray(bufferSize);
    }

}

class BufferedActor_ConcurrentFields_Padding<T extends BufferedActor.ReusableActionContainer> extends BufferedActor_ConcurrentFields<T> {

    long p1, p2, p3, p4, p5, p6, p7, p8,
         p9, p10, p11, p12, p13, p14, p15, p16;

    BufferedActor_ConcurrentFields_Padding(Supplier<T> actionFactory, int bufferSize) {
        super(actionFactory, bufferSize);
    }

}

class BufferedActor_FinalFields<T extends BufferedActor.ReusableActionContainer> extends BufferedActor_ConcurrentFields_Padding<T> {

    final BufferedActor.ReusableActionContainer[] pool;
    final AtomicIntegerArray poolLocks;
    final AtomicReference<BufferedActor.ReusableActionContainer> scheduledActionsStackTop;
    final ReentrantLock lock;
    final int bufferSize;
    final int minBatchSize;
    final int maxBatchSize;
    final Supplier<BufferedActor.ReusableActionContainer> actionFactory;

    BufferedActor_FinalFields(Supplier<T> actionFactory, int bufferSize, int minBatchSize, int maxBatchSize) {
        super(actionFactory, bufferSize);
        this.poolLocks = super.poolLocks;
        this.pool = new BufferedActor.ReusableActionContainer[bufferSize];
        this.bufferSize = bufferSize;

        for (int i = 0; i < bufferSize; i++) {
            BufferedActor.ReusableActionContainer next = actionFactory.get();
            next.index = i;
            pool[next.index] = next;
        }
        this.scheduledActionsStackTop = super.scheduledActionsStackTop;
        this.lock = super.lock;
        this.minBatchSize = minBatchSize;
        this.maxBatchSize = maxBatchSize;
        this.actionFactory = (Supplier<BufferedActor.ReusableActionContainer>) actionFactory;
    }

}

class BufferedActor_FinalFields_Paddind<T extends BufferedActor.ReusableActionContainer> extends BufferedActor_FinalFields<T> {

    long pf1, pf2, pf3, pf4, pf5, pf6, pf7, pf8,
            pf9, pf10, pf11, pf12, pf13, pf14, pf15, pf16;

    BufferedActor_FinalFields_Paddind(Supplier<T> actionFactory, int bufferSize, int minBatchSize, int maxBatchSize) {
        super(actionFactory, bufferSize, minBatchSize, maxBatchSize);
    }

}

/**
 * Primary intention of this class is replacing the blocking by buffering for complex monitoring metrics
 * which can not be implemented(or it is too complex to implement) as none-blocking structure.
 * With BufferedActor you can threat your code as lock-free as long as it is enough place in the buffer,
 * when place in the buffer is left then BufferedActor downgrades processing to the blocking paradigm,
 * but this case should happen rarely because typical application does something useful between metric publishing,
 * in other words it is not likely that your application spent more time in metrics logging instead of doing real work.
 *
 * <p>
 * Second intention is provide garbage-free style of metrics collection.
 * If underling data-structure does not perform memory allocation on each update,
 * then BufferedActor will help to organize message passing in the way that does not require node allocation when placing task to the buffer.
 *
 * <p>
 * Implementation details:
 * <ul>
 *     <li>
 *         At initialization time BufferedActor pre-allocates buffer with actions, then actions are reused.
 *     </li>
 *     <li>
 *         On each time only one thread allowed to do an action.
 *     </li>
 *     <li>
 *         Instead of blocking this class does the buffering of actions if there is a thread in the progress.
 *         That means that computation can be completed somewhere in the future,
 *         but it is not a problem because when you update an metric you never expect the result.
 *     </li>
 *     <li>
 *         Thread in progress does its work in batches instead of applying action one by one, that means that when thread finished its own action
 *     it helps to complete actions scheduled by other threads
 *     </li>
 *     <li>
 *         In order to avoid live lock(when too many parallel treads are scheduling) each active thread is allowed to process no more that {@link #maxBatchSize} actions at once.
 *     </li>
 *     <li>
 *         An thread can be blocked if and only if there are no available actions in the pool, in such case blocked tread helps to process accumulated buffer.
 *     </li>
 * </ul>
 *
 * @param <T>
 */
public class BufferedActor<T extends BufferedActor.ReusableActionContainer> extends BufferedActor_FinalFields_Paddind<T> {

    private ReusableActionContainer scheduledAction;
    private long overflowCounter;

    /**
     * @param actionFactory factory which used to populate action pool, pool is always populated to maximum during initialization
     * @param bufferSize the size of queue for incoming requests
     * @param minBatchSize
     * @param maxBatchSize specifies how many actions one thread is allowed to perform in single batch,
     *                  intent of this parameter is to avoid live lock of exclusive lock owner
     */
    public BufferedActor(Supplier<T> actionFactory, int bufferSize, int minBatchSize, int maxBatchSize) {
        super(actionFactory, bufferSize, minBatchSize, maxBatchSize);
    }

    public final T getActionFromPool() {
        for (int i = 0; i < 2; i++) {
            int index = ThreadLocalRandom.current().nextInt(bufferSize);
            if (poolLocks.compareAndSet(index, 0, 1)) {
                return (T) pool[index];
            }
        }
        return (T) super.actionFactory.get();
    }

    private void offerToPool(ReusableActionContainer action) {
        if (action.isCreatedBecauseOfOverflow()) {
            overflowCounter++;
            return;
        }
        poolLocks.lazySet(action.index, 0);
    }

    public void doExclusivelyOrSchedule(ReusableActionContainer action) {
        AtomicReference<ReusableActionContainer> scheduledActionsStackTop = this.scheduledActionsStackTop;
        while (true) {
            ReusableActionContainer currentTop = scheduledActionsStackTop.get();
            action.next = currentTop;
            action.size = currentTop == null? 1 : currentTop.size + 1;
            if (scheduledActionsStackTop.compareAndSet(currentTop, action)) {
                break;
            }
        }

        ReentrantLock lock = this.lock;
        if (action.isCreatedBecauseOfOverflow()) {
            lock.lock();
        } else if (action.size < minBatchSize) {
            return;
        } else if (!lock.tryLock()) {
            return;
        }

        int batchSize = this.maxBatchSize;
        try {
            int dispatchedActions = 0;
            ReusableActionContainer currentAction = pollScheduledAction(true);
            while (currentAction != null) {
                currentAction.run();
                currentAction.freeGarbage();
                offerToPool(currentAction);
                if (dispatchedActions++ >= batchSize) {
                    break;
                }
                currentAction = pollScheduledAction(false);
            }
        } finally {
            lock.unlock();
        }
    }

    private ReusableActionContainer pollScheduledAction(boolean syncWithQueueAllowed) {
        ReusableActionContainer scheduledAction = this.scheduledAction;
        if (scheduledAction != null) {
            this.scheduledAction = scheduledAction.next;
            scheduledAction.next = null;
            return scheduledAction;
        }

        if (!syncWithQueueAllowed) {
            return null;
        }

        scheduledAction = scheduledActionsStackTop.getAndSet(null);
        if (scheduledAction == null) {
            return null;
        }
        if (scheduledAction.next == null) {
            return scheduledAction;
        }

        // reverse order of stack items to satisfy FIFO queue contract
        ReusableActionContainer previous = scheduledAction;
        ReusableActionContainer current = scheduledAction.next;
        previous.next = null;
        while (current != null) {
            ReusableActionContainer tmp = current.next;
            current.next = previous;
            previous = current;
            current = tmp;
        }

        scheduledAction = previous;
        this.scheduledAction = scheduledAction.next;
        scheduledAction.next = null;
        return scheduledAction;
    }

    public long getOverflowedCount() {
        return overflowCounter;
    }

    public void processAllScheduledActions() {
        ReusableActionContainer action;
        while ((action = pollScheduledAction(true)) != null) {
            action.run();
            action.freeGarbage();
            if (!action.isCreatedBecauseOfOverflow()) {
                offerToPool(action);
            }
        }
    }

    public void clear() {
        ReusableActionContainer action;
        while ((action = pollScheduledAction(true)) != null) {
            action.freeGarbage();
            if (!action.isCreatedBecauseOfOverflow()) {
                offerToPool(action);
            }
        }
    }

    public static abstract class ReusableActionContainer {

        int index = -1;

        int size;

        ReusableActionContainer next;

        public boolean isCreatedBecauseOfOverflow() {
            return index == -1;
        }

        abstract protected void freeGarbage();

        abstract protected void run();

    }

}