/*
 *    Copyright 2017 Vladimir Bukhtoyarov
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

package com.github.rollingmetrics.retention;

import com.github.rollingmetrics.top.TopBuilder;

/**
 * TODO
 *
 * Top configured with this strategy will store all values since the top was created.
 *
 * <p>This is default strategy for {@link TopBuilder}.
 * This strategy is useless for long running applications, because very slow queries happen in the past
 * will not provide chances to fresh queries to take place in the top.
 * So, it is strongly recommended to switch eviction strategy to one of: TODO
 *
 * @return this builder instance
 */
public class UniformRetentionPolicy implements RetentionPolicy {

    public static UniformRetentionPolicy INSTANCE = new UniformRetentionPolicy();

}
