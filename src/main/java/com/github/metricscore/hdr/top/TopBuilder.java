/*
 *    Copyright 2016 Vladimir Bukhtoyarov
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

package com.github.metricscore.hdr.top;

import com.github.metricscore.hdr.top.impl.ResetByChunksTop;
import com.github.metricscore.hdr.top.impl.ResetOnSnapshotConcurrentTop;
import com.github.metricscore.hdr.top.impl.SnapshotCachingTop;
import com.github.metricscore.hdr.top.impl.UniformTop;
import com.github.metricscore.hdr.util.Clock;
import com.github.metricscore.hdr.util.ResilientExecutionUtil;

import java.time.Duration;
import java.util.concurrent.Executor;

public class TopBuilder {

    public static final int MAX_POSITION_COUNT = 1000;
    public static final long MIN_CHUNK_RESETTING_INTERVAL_MILLIS = 1000;
    public static final int MAX_CHUNKS = 25;
    public static final int MIN_LENGTH_OF_QUERY_DESCRIPTION = 10;
    public static final int DEFAULT_MAX_LENGTH_OF_QUERY_DESCRIPTION = 1000;

    public static final Duration DEFAULT_SLOW_QUERY_THRESHOLD = Duration.ZERO;
    public static final Duration DEFAULT_SNAPSHOT_CACHING_DURATION = Duration.ofSeconds(1);

    private static final Executor DEFAULT_BACKGROUND_EXECUTOR = null;
    private static final TopFactory DEFAULT_TOP_FACTORY = TopFactory.UNIFORM;

    private int size;
    private Duration slowQueryThreshold;
    private Duration snapshotCachingDuration;
    private int maxDescriptionLengt;
    private Clock clock;
    private Executor backgroundExecutor;
    private TopFactory factory;

    private TopBuilder(int size, Duration slowQueryThreshold, Duration snapshotCachingDuration, int maxDescriptionLengt, Clock clock, Executor backgroundExecutor, TopFactory factory) {
        this.size = size;
        this.slowQueryThreshold = slowQueryThreshold;
        this.snapshotCachingDuration = snapshotCachingDuration;
        this.maxDescriptionLengt = maxDescriptionLengt;
        this.clock = clock;
        this.backgroundExecutor = backgroundExecutor;
        this.factory = factory;
    }

    public Top build() {
        Top top = factory.create(size, slowQueryThreshold, maxDescriptionLengt, clock);
        if (!snapshotCachingDuration.isZero()) {
            top = new SnapshotCachingTop(top, snapshotCachingDuration.toMillis(), clock);
        }
        return top;
    }

    public static TopBuilder newBuilder(int size) {
        validateSize(size);
        return new TopBuilder(size, DEFAULT_SLOW_QUERY_THRESHOLD, DEFAULT_SNAPSHOT_CACHING_DURATION, DEFAULT_MAX_LENGTH_OF_QUERY_DESCRIPTION, Clock.defaultClock(), DEFAULT_BACKGROUND_EXECUTOR, DEFAULT_TOP_FACTORY);
    }

    public TopBuilder withPositionCount(int size) {
        validateSize(size);
        this.size = size;
        return this;
    }

    public TopBuilder withSlowQueryThreshold(Duration slowQueryThreshold) {
        if (slowQueryThreshold == null) {
            throw new IllegalArgumentException("slowQueryThreshold should not be null");
        }
        if (slowQueryThreshold.isNegative()) {
            throw new IllegalArgumentException("slowQueryThreshold should not be negative");
        }
        this.slowQueryThreshold = slowQueryThreshold;
        return this;
    }

    public TopBuilder withSnapshotCachingDuration(Duration snapshotCachingDuration) {
        if (snapshotCachingDuration == null) {
            throw new IllegalArgumentException("snapshotCachingDuration should not be null");
        }
        if (snapshotCachingDuration.isNegative()) {
            throw new IllegalArgumentException("snapshotCachingDuration can not be negative");
        }
        this.snapshotCachingDuration = snapshotCachingDuration;
        return this;
    }

    public TopBuilder withMaxLengthOfQueryDescription(int maxLengthOfQueryDescription) {
        if (maxLengthOfQueryDescription < MIN_LENGTH_OF_QUERY_DESCRIPTION) {
            String msg = "The requested maxDescriptionLengt=" + maxLengthOfQueryDescription + " is wrong " +
                    "because of maxDescriptionLengt should be >=" + MIN_LENGTH_OF_QUERY_DESCRIPTION + "." +
                    "How do you plan to distinguish one query from another with so short description?";
            throw new IllegalArgumentException(msg);
        }
        this.maxDescriptionLengt = maxLengthOfQueryDescription;
        return this;
    }

    public TopBuilder withClock(Clock clock) {
        if (clock == null) {
            throw new IllegalArgumentException("Clock should not be null");
        }
        this.clock = clock;
        return this;
    }

    public TopBuilder withBackgroundExecutor(Executor backgroundExecutor) {
        if (backgroundExecutor == null) {
            throw new IllegalArgumentException("Clock should not be null");
        }
        this.backgroundExecutor = backgroundExecutor;
        return this;
    }

    public TopBuilder neverResetPostions() {
        this.factory = TopFactory.UNIFORM;
        return this;
    }

    public TopBuilder resetAllPositionsOnSnapshot() {
        this.factory = TopFactory.RESET_ON_SNAPSHOT;
        return this;
    }

    public TopBuilder resetAllPositionsPeriodically(Duration intervalBetweenResetting) {
        if (intervalBetweenResetting == null) {
            throw new IllegalArgumentException("intervalBetweenResetting should not be null");
        }
        if (intervalBetweenResetting.isNegative()) {
            throw new IllegalArgumentException("intervalBetweenResetting should not be negative");
        }
        long intervalBetweenResettingMillis = intervalBetweenResetting.toMillis();
        if (intervalBetweenResettingMillis < MIN_CHUNK_RESETTING_INTERVAL_MILLIS) {
            throw new IllegalArgumentException("");
        }
        this.factory = resetByChunks(intervalBetweenResettingMillis, 0);
        return this;
    }

    public TopBuilder resetAllPositionsPeriodicallyByChunks(Duration rollingWindow, int numberChunks) {
        if (numberChunks > MAX_CHUNKS) {
            throw new IllegalArgumentException("numberChunks should be <= " + MAX_CHUNKS);
        }
        if (numberChunks < 2) {
            throw new IllegalArgumentException("numberChunks should be >= 1");
        }
        if (rollingWindow == null) {
            throw new IllegalArgumentException("rollingWindow should not be null");
        }
        if (rollingWindow.isNegative()) {
            throw new IllegalArgumentException("rollingWindow should not be negative");
        }

        long intervalBetweenResettingMillis = rollingWindow.toMillis() / numberChunks;
        if (intervalBetweenResettingMillis < MIN_CHUNK_RESETTING_INTERVAL_MILLIS) {
            String msg = "interval between resetting one chunk should be >= " + MIN_CHUNK_RESETTING_INTERVAL_MILLIS + " millis";
            throw new IllegalArgumentException(msg);
        }
        this.factory = resetByChunks(intervalBetweenResettingMillis, numberChunks);
        return this;
    }

    @Override
    public TopBuilder clone() {
        return new TopBuilder(size, slowQueryThreshold, snapshotCachingDuration, maxDescriptionLengt, clock, backgroundExecutor, factory);
    }

    private interface TopFactory {

        Top create(int size, Duration slowQueryThreshold, int maxDescriptionLength, Clock clock);

        TopFactory UNIFORM = new TopFactory() {
            @Override
            public Top create(int size, Duration slowQueryThreshold, int maxDescriptionLength, Clock clock) {
                return new UniformTop(size, slowQueryThreshold.toNanos(), maxDescriptionLength);
            }
        };

        TopFactory RESET_ON_SNAPSHOT = new TopFactory() {
            @Override
            public Top create(int size, Duration slowQueryThreshold, int maxDescriptionLength, Clock clock) {
                return new ResetOnSnapshotConcurrentTop(size, slowQueryThreshold.toNanos(), maxDescriptionLength);
            }
        };

    }

    private static void validateSize(int size) {
        if (size < 1) {
            throw new IllegalArgumentException("size should be >=1");
        }
        if (size > MAX_POSITION_COUNT) {
            throw new IllegalArgumentException("size should be <= " + MAX_POSITION_COUNT);
        }
    }

    private TopFactory resetByChunks(final long intervalBetweenResettingMillis, int numberOfHistoryChunks) {
        return new TopFactory() {
            @Override
            public Top create(int size, Duration slowQueryThreshold, int maxDescriptionLength, Clock clock) {
                return new ResetByChunksTop(size, slowQueryThreshold.toNanos(), maxDescriptionLength, intervalBetweenResettingMillis, numberOfHistoryChunks, clock, getExecutor());
            }
        };
    }

    private Executor getExecutor() {
        return backgroundExecutor != null ? backgroundExecutor : ResilientExecutionUtil.getInstance().getBackgroundExecutor();
    }

}
