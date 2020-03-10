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

import org.jctools.queues.MpscArrayQueue;
import org.jctools.queues.SpmcArrayQueue;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

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
public class BufferedActor<T extends BufferedActor.ReusableActionContainer> {

    private final SpmcArrayQueue<T> actionPool;
    private final MpscArrayQueue<T> scheduledActions;

    private final ReentrantLock lock = new ReentrantLock();
    private final AtomicLong blockedCounter = new AtomicLong();
    private final int batchSize;

    /**
     * @param actionFactory factory which used to populate action pool, pool is always populated to maximum during initialization
     * @param bufferSize the size of queue for incoming requests
     * @param batchSize specifies how many actions one thread is allowed to perform in single batch,
     *                  intent of this parameter is to avoid live lock of exclusive lock owner
     */
    public BufferedActor(Supplier<T> actionFactory, int bufferSize, int batchSize) {
        actionPool = new SpmcArrayQueue<>(bufferSize);
        scheduledActions = new MpscArrayQueue<>(bufferSize);
        this.batchSize = batchSize;

        for (int i = 0; i < bufferSize; i++) {
            T action = actionFactory.get();
            actionPool.add(action);
        }
    }

    public final T getActionFromPool() {
        T action = actionPool.poll();
        if (action != null) {
            // fast path
            return action;
        }

        // if there are no actions in pool we need to help to process already scheduled actions
        blockedCounter.incrementAndGet();
        lock.lock();

        boolean actionFromPool = false;
        T firstAction = null;
        while (firstAction == null) {
            firstAction = actionPool.poll();
            if (firstAction != null) {
                actionFromPool = true;
                break;
            }
            firstAction = scheduledActions.poll();
        }

        int dispatchedActions = 0;
        T currentAction = firstAction;
        while (currentAction != null) {
            if (currentAction != firstAction || !actionFromPool) {
                currentAction.run();
            }
            if (currentAction != firstAction) {
                currentAction.freeGarbage();
                actionPool.offer(currentAction);
            }
            if (dispatchedActions++ >= batchSize) {
                break;
            }
            currentAction = scheduledActions.poll();
        }
        lock.unlock();

        firstAction.freeGarbage();
        return firstAction;
    }

    public void doExclusivelyOrSchedule(T action) {
        scheduledActions.offer(action);

        if (!lock.tryLock()) {
            return;
        }

        int dispatchedActions = 0;

        T firstAction = scheduledActions.poll();
        T currentAction = firstAction;

        while (currentAction != null) {
            currentAction.run();
            if (currentAction != firstAction) {
                currentAction.freeGarbage();
                actionPool.offer(currentAction);
            }
            if (dispatchedActions++ >= batchSize) {
                break;
            }
            currentAction = scheduledActions.poll();
        }

        if (firstAction != null) {
            firstAction.freeGarbage();
            actionPool.offer(firstAction);
        }
        lock.unlock();
    }

    public long getBlockedCount() {
        return blockedCounter.get();
    }

    public int getActionPoolSize() {
        return actionPool.size();
    }

    public int getScheduledActionsCount() {
        return scheduledActions.size();
    }

    public void processAllScheduladActions() {
        T action = scheduledActions.poll();
        while (action != null) {
            action.run();
            action.freeGarbage();
            actionPool.offer(action);
        }
    }

    public void clear() {
        T action = scheduledActions.poll();
        while (action != null) {
            action.freeGarbage();
            actionPool.offer(action);
        }
    }

    public static abstract class ReusableActionContainer {

        private ReusableActionContainer previous;

        abstract protected void freeGarbage();

        abstract protected void run();

    }

}