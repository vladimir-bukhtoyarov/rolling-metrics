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

package com.github.rollingmetrics.top;

import com.github.rollingmetrics.util.ResilientExecutionUtil;

import java.time.Duration;
import java.util.concurrent.Executor;

public class TopRecorderSettings {

    private static final Executor DEFAULT_BACKGROUND_EXECUTOR = null;
    public static final int MAX_POSITION_COUNT = 1000;
    public static final int DEFAULT_MAX_LENGTH_OF_QUERY_DESCRIPTION = 1000;
    public static final Duration DEFAULT_LATENCY_THRESHOLD = Duration.ZERO;
    public static final int MIN_LENGTH_OF_QUERY_DESCRIPTION = 10;

    private int size;
    private Duration latencyThreshold;
    private int maxDescriptionLength;
    private Executor backgroundExecutor;
    private int maxLengthOfQueryDescription;

    public TopRecorderSettings(int size) {
        validateSize(size);
        this.size = size;
        this.latencyThreshold = DEFAULT_LATENCY_THRESHOLD;
        this.maxDescriptionLength = DEFAULT_MAX_LENGTH_OF_QUERY_DESCRIPTION;
        this.backgroundExecutor = DEFAULT_BACKGROUND_EXECUTOR;
    }

    public Executor getExecutor() {
        return backgroundExecutor != null ? backgroundExecutor : ResilientExecutionUtil.getInstance().getBackgroundExecutor();
    }

    public int getSize() {
        return size;
    }

    public Duration getLatencyThreshold() {
        return latencyThreshold;
    }

    public int getMaxDescriptionLength() {
        return maxDescriptionLength;
    }

    public Executor getBackgroundExecutor() {
        return backgroundExecutor;
    }

    public void setBackgroundExecutor(Executor backgroundExecutor) {
        if (backgroundExecutor == null) {
            throw new IllegalArgumentException("backgroundExecutor should not be null");
        }
        this.backgroundExecutor = backgroundExecutor;
    }

    public void setLatencyThreshold(Duration latencyThreshold) {
        if (latencyThreshold == null) {
            throw new IllegalArgumentException("latencyThreshold should not be null");
        }
        if (latencyThreshold.isNegative()) {
            throw new IllegalArgumentException("latencyThreshold should not be negative");
        }
        this.latencyThreshold = latencyThreshold;
    }

    public void setSize(int size) {
        validateSize(size);
        this.size = size;
    }

    public void setMaxLengthOfQueryDescription(int maxLengthOfQueryDescription) {
        if (maxLengthOfQueryDescription < MIN_LENGTH_OF_QUERY_DESCRIPTION) {
            String msg = "The requested maxDescriptionLength=" + maxLengthOfQueryDescription + " is wrong " +
                    "because of maxDescriptionLength should be >=" + MIN_LENGTH_OF_QUERY_DESCRIPTION + "." +
                    "How do you plan to distinguish one query from another with so short description?";
            throw new IllegalArgumentException(msg);
        }
        this.maxDescriptionLength = maxLengthOfQueryDescription;
    }

    private static void validateSize(int size) {
        if (size < 1) {
            throw new IllegalArgumentException("size should be >=1");
        }
        if (size > MAX_POSITION_COUNT) {
            throw new IllegalArgumentException("size should be <= " + MAX_POSITION_COUNT);
        }
    }
}
