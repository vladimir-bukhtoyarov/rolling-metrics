/*
 *
 *  Copyright 2017 Vladimir Bukhtoyarov
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

package com.github.rollingmetrics.top;


import java.util.List;

/**
 * The top of queries sorted by its latency.
 * The top is sized, independent of count of recorded queries, the top always stores no more than {@link #getSize} positions,
 * the longer queries displace shorter queries when ranking reaches it max size.
 */
public interface Ranking {

    /**
     * Creates new instance of {@link RankingBuilder}
     *
     * @param size maximum count of positions in the top
     *
     * @return new instance of {@link RankingBuilder}
     */
    static RankingBuilder builder(int size) {
        return RankingBuilder.newBuilder(size);
    }

    /**
     * Updates the ranking positions.
     *
     * @param weight the weight of query
     * @param identity the identity by which one query can be distinguished from another.
     */
    void update(long weight, Object identity);

    /**
     * Returns the top of queries in descend order, slowest query will be at first place.
     * The size of returned list can be less then {@link #getSize} if not enough count of quires were recorded.
     *
     * @return the top of queries in descend order.
     */
    List<Position> getPositionsInDescendingOrder();

    /**
     * @return the maximum count of positions that can be stored in the ranking.
     */
    int getSize();

}
