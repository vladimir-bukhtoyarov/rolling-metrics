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

import java.util.concurrent.atomic.AtomicReference;

/**
 * An example of classic Michael-Scott queue
 *
 * @param <T>
 */
public class MSQueue<T> {

    private final class Node<T> {

        // TODO it could be replaced by AtomicFieldUpdater in order to reduce memory footprint,
        //  but I especially avoided to do this because intent of MSQueue is just to be a learning eaxample
        private final AtomicReference<Node<T>> nextRef;

        private T item;

        private Node(T item, Node<T> next) {
            this.nextRef = new AtomicReference<>(next);
            this.item = item;
        }

    }

    private final AtomicReference<Node<T>> tailRef = new AtomicReference<>(new Node<>(null, null));
    private final AtomicReference<Node<T>> headRef = new AtomicReference<>(tailRef.get());

    public final void put(T item) {
        Node<T> newNode = new Node<T>(item, null);
        while (true) {
            Node<T> currentTail = tailRef.get();
            Node<T> nextTail = currentTail.nextRef.get();
            if (currentTail == tailRef.get()) {
                if (nextTail != null) {
                    tailRef.compareAndSet(currentTail, nextTail);
                } else {
                    if (currentTail.nextRef.compareAndSet(null, newNode)) {
                        tailRef.compareAndSet(currentTail, newNode);
                        return;
                    }
                }
            }
        }
    }

    public final T poll() {
        while (true) {
            Node<T> currentHead = headRef.get();
            Node<T> currentTail = tailRef.get();
            Node<T> next = currentHead.nextRef.get();
            if (currentHead == headRef.get()) {
                if (currentHead == currentTail) {
                    if (next == null) {
                        return null;
                    } else {
                        // help to producer
                        tailRef.compareAndSet(currentTail, next);
                    }
                } else {
                    if (headRef.compareAndSet(currentHead, next)) {
                        T item = next.item;
                        next.item = null; // help to gc
                        return item;
                    }
                }
            }
        }
    }

}
