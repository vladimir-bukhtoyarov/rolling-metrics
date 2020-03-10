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

package com.github.rollingmetrics.top.impl.recorder;

import com.github.rollingmetrics.top.Position;

import java.util.*;

/**
 * Is not a part of public API, this class just used as building block for high-level Top implementations.
 *
 * This implementation does not support concurrent access at all, synchronization aspects should be managed outside.
 */
public class SingleThreadedRanking {

    private final Object[] identities;
    private final long[] weights;
    private final int maxSize;
    private final long threshold;

    private int currentSize;

    public SingleThreadedRanking(int maxSize, long threshold) {
        this.identities = new Object[maxSize];
        this.weights = new long[maxSize];

        this.maxSize = maxSize;
        this.threshold = threshold;
    }

    public UpdateResult update(long weight, Object identity) {
        if (currentSize == 0) {
            // corner case for empty collection
            insert(weight, identity);
            return UpdateResult.INSERTED;
        }

        boolean full = currentSize == maxSize;
        if (full && weight < weights[0]) {
            return UpdateResult.SKIPPED_BECAUSE_TOO_SMALL;
        }

        // try to find duplicate
        int indexToInsert = -1;
        int indexOfDuplicateForRemoval = -1;

        for (int i = 0; i < currentSize; i++) {
            long existedWeight = weights[i];
            Object existedIdentity = identities[i];
            if (indexOfDuplicateForRemoval == -1 && existedIdentity.equals(identity)) {
                if (weight <= existedWeight) {
                    return UpdateResult.SKIPPED_BECAUSE_OF_DUPLICATE;
                } else {
                    indexOfDuplicateForRemoval = i;
                    indexToInsert = i;
                }
            }

            if (weight < existedWeight) {
                if (indexOfDuplicateForRemoval != -1) {
                    break;
                }
            } else {
                indexToInsert = i;
            }
        }

        if (indexToInsert == -1) {
            if (!full) {
                positions.add(0, asPosition());
            }
            return;
        }

        if (indexOfDuplicateForRemoval != -1) {
            if (indexToInsert == indexOfDuplicateForRemoval) {
                positions.set(indexOfDuplicateForRemoval, asPosition());
            } else {
                positions.remove(indexOfDuplicateForRemoval);
                positions.add(indexToInsert, asPosition());
            }
        }

        if (!full) {
            positions.add(indexToInsert + 1, asPosition());
        } else {
            if (indexToInsert == 0) {
                positions.set(0, asPosition());
            } else {
                positions.remove(0);
                positions.add(indexToInsert, asPosition());
            }
        }
    }

    private void insert(int index, long weight, Object identity) {
        weights[currentSize] = weight;
        identities[currentSize] = identity;
        currentSize++;
    }

    private void insert(long weight, Object identity) {
        weights[currentSize] = weight;
        identities[currentSize] = identity;
        currentSize++;
    }

    public void addInto(SingleThreadedRanking other) {
        for (int i = 0; i < currentSize; i++) {
            if (other.update(weights[i], identities[i]) == UpdateResult.SKIPPED_BECAUSE_TOO_SMALL) {
                return;
            }
        }
    }

    public void reset() {
        currentSize = 0;
        Arrays.fill(identities, 0, maxSize - 1, null);
    }

    public long getThreshold() {
        return threshold;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public List<Position> getPositionsInDescendingOrder() {
        if (positions.isEmpty()) {
            return Collections.emptyList();
        }
        ArrayList<Position> result = new ArrayList<>(positions.size());
        result.addAll(positions.descendingSet());
        return result;
    }

    static boolean isNeedToAdd(Position newPosition, Position currentMinimum) {
        if (currentMinimum == null) {
            return true;
        }
        if (newPosition.getLatencyInNanoseconds() > currentMinimum.getLatencyInNanoseconds()) {
            return true;
        }
        if (newPosition.getLatencyInNanoseconds() == currentMinimum.getLatencyInNanoseconds()) {
            return newPosition.getTimestamp() > currentMinimum.getTimestamp();
        }
        return false;
    }

    static enum UpdateResult {

        SKIPPED_BECAUSE_TOO_SMALL, SKIPPED_BECAUSE_OF_DUPLICATE, INSERTED;

    }

}
