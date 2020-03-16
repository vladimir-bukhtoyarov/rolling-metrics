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

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;


class BufferedActor_SharedFields<T extends BufferedActor.ReusableActionContainer> {

    final BufferedActor.ReusableActionContainer[] pool;
    final AtomicLong actionPoolHead;
    final AtomicReference<BufferedActor.ReusableActionContainer> scheduledActionsStackTop;

    final ReentrantLock lock = new ReentrantLock();

    BufferedActor_SharedFields(Supplier<T> actionFactory, int bufferSize) {
        scheduledActionsStackTop = new AtomicReference<>();

        this.pool = new BufferedActor.ReusableActionContainer[bufferSize];
        pool[0] = actionFactory.get();
        pool[0].index = 0;
        this.actionPoolHead = new AtomicLong(BufferedActor.toSequencedPointer(pool[0].index, 0));

        BufferedActor.ReusableActionContainer previous = pool[0];
        for (int i = 1; i < bufferSize; i++) {
            BufferedActor.ReusableActionContainer next = actionFactory.get();
            next.index = i;
            pool[next.index] = next;
            previous.poolNext = next;
            previous = next;
        }
    }

}

class BufferedActor_SharedFields_Padding<T extends BufferedActor.ReusableActionContainer> extends BufferedActor_SharedFields<T> {

    long p1, p2, p3, p4, p5, p6, p7, p8,
         p9, p10, p11, p12, p13, p14, p15, p16;

    BufferedActor_SharedFields_Padding(Supplier actionFactory, int bufferSize) {
        super(actionFactory, bufferSize);
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
 *         In order to avoid live lock(when too many parallel treads are scheduling) each active thread is allowed to process no more that {@link #batchSize} actions at once.
 *     </li>
 *     <li>
 *         An thread can be blocked if and only if there are no available actions in the pool, in such case blocked tread helps to process accumulated buffer.
 *     </li>
 * </ul>
 *
 * @param <T>
 */
public class BufferedActor<T extends BufferedActor.ReusableActionContainer> extends BufferedActor_SharedFields_Padding<T> {

    private final int batchSize;
    private final Supplier<T> actionFactory;

    private volatile ReusableActionContainer actionPoolTail;

    private ReusableActionContainer scheduledAction;
    private long overflowCounter;

    /**
     * @param actionFactory factory which used to populate action pool, pool is always populated to maximum during initialization
     * @param bufferSize the size of queue for incoming requests
     * @param batchSize specifies how many actions one thread is allowed to perform in single batch,
     *                  intent of this parameter is to avoid live lock of exclusive lock owner
     */
    public BufferedActor(Supplier<T> actionFactory, int bufferSize, int batchSize) {
        super(actionFactory, bufferSize);
        this.batchSize = batchSize;
        this.actionFactory = actionFactory;
        actionPoolTail = pool[bufferSize - 1];
    }

    public final T getActionFromPool() {
        ReusableActionContainer action = pollFromPool();
        if (action == null) {
            action = actionFactory.get();
            action.createdBecauseOfOverflow = true;
        }
        return (T) action;
    }

    private ReusableActionContainer pollFromPool() {
        AtomicLong actionPoolHead = this.actionPoolHead;
        ReusableActionContainer[] pool = this.pool;
        while (true) {
            long sequencedHeadPointer = actionPoolHead.get();
            int seq = getSequence(sequencedHeadPointer);
            int index = getIndex(sequencedHeadPointer);

            ReusableActionContainer currentHead = pool[index];
            ReusableActionContainer currentTail = actionPoolTail;
            if (sequencedHeadPointer != actionPoolHead.get()) {
                continue;
            }

            if (currentHead == currentTail) {
                // last node used just as Hazard Pointer, we never return it
                return null;
            }
            ReusableActionContainer nextHead = currentHead.poolNext;
            if (nextHead == null) {
                continue;
            }

            long nextSequencedHeadPointer = toSequencedPointer(nextHead.index, seq + 1);
            if (actionPoolHead.compareAndSet(sequencedHeadPointer, nextSequencedHeadPointer)) {
                return currentHead;
            }
        }
    }

    private void offerToPool(ReusableActionContainer action) {
        if (action.isCreatedBecauseOfOverflow()) {
            overflowCounter++;
            return;
        }
        ReusableActionContainer currentTail = actionPoolTail;
        currentTail.poolNext = action;
        actionPoolTail = action;
    }

    public void doExclusivelyOrSchedule(ReusableActionContainer action) {
        AtomicReference<ReusableActionContainer> scheduledActionsStackTop = this.scheduledActionsStackTop;
        while (true) {
            ReusableActionContainer currentTop = scheduledActionsStackTop.get();
            action.workQueueNext = currentTop;
            if (scheduledActionsStackTop.compareAndSet(currentTop, action)) {
                break;
            }
        }

        ReentrantLock lock = this.lock;
        if (action.isCreatedBecauseOfOverflow()) {
            lock.lock();
        } else if (!lock.tryLock()) {
            return;
        }

        int batchSize = this.batchSize;
        try {
            int dispatchedActions = 0;
            ReusableActionContainer currentAction;
            while ((currentAction = pollScheduledAction(scheduledActionsStackTop)) != null) {
                currentAction.run();
                currentAction.freeGarbage();
                offerToPool(currentAction);
                if (dispatchedActions++ >= batchSize) {
                    break;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private ReusableActionContainer pollScheduledAction(AtomicReference<ReusableActionContainer> scheduledActionsStackTop) {
        ReusableActionContainer scheduledAction = this.scheduledAction;
        if (scheduledAction != null) {
            this.scheduledAction = scheduledAction.workQueueNext;
            scheduledAction.workQueueNext = null;
            return scheduledAction;
        }

        scheduledAction = scheduledActionsStackTop.getAndSet(null);
        if (scheduledAction == null) {
            return null;
        }
        if (scheduledAction.workQueueNext == null) {
            return scheduledAction;
        }

        // reverse order of stack items to satisfy FIFO queue contract
        ReusableActionContainer previous = scheduledAction;
        ReusableActionContainer current = scheduledAction.workQueueNext;
        previous.workQueueNext = null;
        while (current != null) {
            ReusableActionContainer tmp = current.workQueueNext;
            current.workQueueNext = previous;
            previous = current;
            current = tmp;
        }

        scheduledAction = previous;
        this.scheduledAction = scheduledAction.workQueueNext;
        scheduledAction.workQueueNext = null;
        return scheduledAction;
    }

    public long getOverflowedCount() {
        return overflowCounter;
    }

    public void processAllScheduledActions() {
        AtomicReference<ReusableActionContainer> scheduledActionsStackTop = this.scheduledActionsStackTop;

        ReusableActionContainer action;
        while ((action = pollScheduledAction(scheduledActionsStackTop)) != null) {
            action.run();
            action.freeGarbage();
            if (!action.isCreatedBecauseOfOverflow()) {
                offerToPool(action);
            }
        }
    }

    public void clear() {
        AtomicReference<ReusableActionContainer> scheduledActionsStackTop = this.scheduledActionsStackTop;

        ReusableActionContainer action;
        while ((action = pollScheduledAction(scheduledActionsStackTop)) != null) {
            action.freeGarbage();
            if (!action.isCreatedBecauseOfOverflow()) {
                offerToPool(action);
            }
        }
    }

    public static abstract class ReusableActionContainer {

        int index;

        boolean createdBecauseOfOverflow;

        ReusableActionContainer poolNext;
        ReusableActionContainer workQueueNext;

        public boolean isCreatedBecauseOfOverflow() {
            return createdBecauseOfOverflow;
        }

        abstract protected void freeGarbage();

        abstract protected void run();

    }

    static int getIndex(long sequentialPointer) {
        return (int) (sequentialPointer >> 32);
    }

    static int getSequence(long sequentialPointer) {
        return (int) sequentialPointer;
    }

    static long toSequencedPointer(int index, int sequence) {
        return (long) index << 32 | sequence & 0xFFFFFFFFL;
    }

}