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

import org.junit.Test;

import java.time.Duration;

public class DefaultRetentionPolicyTest {

    @Test(expected = IllegalArgumentException.class)
    public void nullExecutorShouldBeDeprecated() {
        new DefaultRetentionPolicy(){}.withBackgroundExecutor(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullTickerShouldBeDisallowed() {
        new DefaultRetentionPolicy(){}.withTicker(null);
    }

    @Test
    public void shouldAllowZeroCachingDuration() {
        new DefaultRetentionPolicy(){}.withSnapshotCachingDuration(Duration.ZERO);
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeCachingDurationShouldBeDisallowed() {
        new DefaultRetentionPolicy(){}.withSnapshotCachingDuration(Duration.ofMillis(-2000));
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullCachingDurationShouldBeDisallowed() {
        new DefaultRetentionPolicy(){}.withSnapshotCachingDuration(null);
    }

}