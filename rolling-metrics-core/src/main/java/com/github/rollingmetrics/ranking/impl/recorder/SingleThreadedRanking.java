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

package com.github.rollingmetrics.ranking.impl.recorder;

import com.github.rollingmetrics.ranking.Position;

import java.util.*;

/**
 * Is not a part of public API, this class just used as building block for high-level Ranking implementations.
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
        return update(weight, identity, true);
    }

    private UpdateResult update(long weight, Object identity, boolean freshReplacesOldWithSameWeight) {
        if (weight < threshold) {
            return UpdateResult.SKIPPED_BECAUSE_TOO_SMALL;
        }
        if (currentSize == 0) {
            // corner case for empty collection
            add(weight, identity);
            return UpdateResult.INSERTED;
        }

        boolean full = currentSize == maxSize;
        if (full && weight < weights[0]) {
            return UpdateResult.SKIPPED_BECAUSE_TOO_SMALL;
        }

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

            if (weight > existedWeight || (freshReplacesOldWithSameWeight && weight == existedWeight)) {
                indexToInsert = i;
            } else if (indexOfDuplicateForRemoval != -1) {
                break;
            }
        }

        if (indexToInsert == -1) {
            if (!full) {
                add(0, weight, identity);
                return UpdateResult.INSERTED;
            }
            return UpdateResult.SKIPPED_BECAUSE_TOO_SMALL;
        }

        if (indexOfDuplicateForRemoval != -1) {
            if (indexToInsert == indexOfDuplicateForRemoval) {
                set(indexOfDuplicateForRemoval, weight, identity);
            } else {
                remove(indexOfDuplicateForRemoval);
                add(indexToInsert, weight, identity);
            }
        }

        if (!full) {
            add(indexToInsert + 1, weight, identity);
        } else {
            if (indexToInsert == 0) {
                set(0, weight, identity);
            } else {
                remove(0);
                add(indexToInsert, weight, identity);
            }
        }
        return UpdateResult.INSERTED;
    }

    private void remove(int index) {
        int numMoved = currentSize - index - 1;
        if (numMoved > 0) {
            System.arraycopy(weights, index + 1, weights, index, numMoved);
            System.arraycopy(identities, index + 1, identities, index, numMoved);
            identities[currentSize - 1] = null;
        }
        currentSize--;
    }

    private void set(int index, long weight, Object identity) {
        weights[index] = weight;
        identities[index] = identity;
    }

    private void add(int index, long weight, Object identity) {
        System.arraycopy(weights, index, weights, index + 1, currentSize - index);
        weights[index] = weight;

        System.arraycopy(identities, index, identities, index + 1, currentSize - index);
        identities[index] = identity;

        currentSize++;
    }

    private void add(long weight, Object identity) {
        weights[currentSize] = weight;
        identities[currentSize] = identity;
        currentSize++;
    }

    public void addInto(SingleThreadedRanking other) {
        for (int i = currentSize - 1; i >= 0; i--) {
            if (other.update(weights[i], identities[i], false) == UpdateResult.SKIPPED_BECAUSE_TOO_SMALL) {
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
        if (currentSize == 0) {
            return Collections.emptyList();
        }
        ArrayList<Position> result = new ArrayList<>(currentSize);
        for (int i = currentSize - 1; i >= 0 ; i--) {
            result.add(new Position(weights[i], identities[i]));
        }
        return result;
    }

    public enum UpdateResult {

        SKIPPED_BECAUSE_TOO_SMALL, SKIPPED_BECAUSE_OF_DUPLICATE, INSERTED;

    }

    @Override
    public String toString() {
        return getPositionsInDescendingOrder().toString();
    }

}
