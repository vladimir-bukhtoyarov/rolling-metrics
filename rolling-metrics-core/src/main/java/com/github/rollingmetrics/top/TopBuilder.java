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

import com.github.rollingmetrics.top.impl.TopRecorderSettings;

import java.time.Duration;

/**
 * The builder for {@link Top}.
 *
 * <p><br> Basic examples of usage:
 * <pre> {@code
 *
 *  Top top = Top.builder(3).resetAllPositionsOnSnapshot().create();
 *  MetricSet metricSet = new TopMetricSet("my-top", top, TimeUnit.MILLISECONDS, 5);
 *  registry.registerAll(metricSet);
 * }</pre>
 *
 * @see Top
 */
public interface TopBuilder {

    /**
     * Constructs new {@link Top} instance
     *
     * @return new {@link Top} instance
     */
    Top build();

    /**
     * Configures the maximum count of positions for tops which will be constructed by this builder.
     *
     * @param size the maximum count of positions
     * @return this builder instance
     */
    TopBuilder withPositionCount(int size);

    /**
     * Configures the latency threshold. The queries having latency which shorter than threshold, will not be tracked in the top.
     * The default value is zero {@link TopRecorderSettings#DEFAULT_LATENCY_THRESHOLD}, means that all queries can be recorded independent of its latency.
     *
     * Specify this parameter when you want not to track queries which fast,
     * in other words when you want see nothing when all going well.
     *
     * @param latencyThreshold
     * @return this builder instance
     */
    TopBuilder withLatencyThreshold(Duration latencyThreshold);

    /**
     * Specifies the max length of description position int the top. The characters upper {@code maxLengthOfQueryDescription} limit will be truncated
     *
     * <p>
     * The default value is 1000 symbol {@link TopRecorderSettings#DEFAULT_MAX_LENGTH_OF_QUERY_DESCRIPTION}.
     *
     * @param maxLengthOfQueryDescription the max length of description position int the top.
     *
     * @return this builder instance
     */
    TopBuilder withMaxLengthOfQueryDescription(int maxLengthOfQueryDescription);

}
