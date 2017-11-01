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

package com.github.rollingmetrics;

import com.github.rollingmetrics.counter.WindowCounter;
import com.github.rollingmetrics.histogram.hdr.RecorderSettings;
import com.github.rollingmetrics.histogram.hdr.RollingHdrHistogram;
import com.github.rollingmetrics.hitratio.HitRatio;
import com.github.rollingmetrics.top.Top;
import com.github.rollingmetrics.top.TopSettings;
import com.github.rollingmetrics.util.Ticker;

import java.time.Duration;

/**
 * TODO javadocs
 */
public interface RetentionPolicy {

    static RetentionPolicy uniform() {
        return new RetentionPolicy() {
            @Override
            public WindowCounter newCounter(Ticker ticker) {
                // TODO
                return null;
            }

            @Override
            public HitRatio newHitRatio(Ticker ticker) {
                // TODO
                return null;
            }

            @Override
            public Top newTop(TopSettings settings, Ticker ticker) {
                // TODO
                return null;
            }

            @Override
            public RollingHdrHistogram newHistogram(RecorderSettings settings, Ticker ticker) {
                // TODO
                return null;
            }
        };
    }

    static RetentionPolicy resetOnSnapshot() {
        return new RetentionPolicy() {
            @Override
            public WindowCounter newCounter(Ticker ticker) {
                // TODO
                return null;
            }

            @Override
            public HitRatio newHitRatio(Ticker ticker) {
                // TODO
                return null;
            }

            @Override
            public Top newTop(TopSettings settings, Ticker ticker) {
                // TODO
                return null;
            }

            @Override
            public RollingHdrHistogram newHistogram(RecorderSettings settings, Ticker ticker) {
                // TODO
                return null;
            }
        };
    }

    static RetentionPolicy resetOnCondition(ResetCondition condition) {
        return new RetentionPolicy() {
            @Override
            public WindowCounter newCounter(Ticker ticker) {
                // TODO
                return null;
            }

            @Override
            public HitRatio newHitRatio(Ticker ticker) {
                // TODO
                return null;
            }

            @Override
            public Top newTop(TopSettings settings, Ticker ticker) {
                // TODO
                return null;
            }

            @Override
            public RollingHdrHistogram newHistogram(RecorderSettings settings, Ticker ticker) {
                // TODO
                return null;
            }
        };
    }

    static RetentionPolicy resetPeriodically(Duration resetPeriod) {
        return new RetentionPolicy() {
            @Override
            public WindowCounter newCounter(Ticker ticker) {
                // TODO
                return null;
            }

            @Override
            public HitRatio newHitRatio(Ticker ticker) {
                // TODO
                return null;
            }

            @Override
            public Top newTop(TopSettings settings, Ticker ticker) {
                // TODO
                return null;
            }

            @Override
            public RollingHdrHistogram newHistogram(RecorderSettings settings, Ticker ticker) {
                // TODO
                return null;
            }
        };
    }

    static RetentionPolicy resetPositionsPeriodicallyByChunks(Duration rollingTimeWindow, int numberChunks) {
        return new RetentionPolicy() {
            @Override
            public WindowCounter newCounter(Ticker ticker) {
                // TODO
                return null;
            }

            @Override
            public HitRatio newHitRatio(Ticker ticker) {
                // TODO
                return null;
            }

            @Override
            public Top newTop(TopSettings settings, Ticker ticker) {
                // TODO
                return null;
            }

            @Override
            public RollingHdrHistogram newHistogram(RecorderSettings settings, Ticker ticker) {
                // TODO
                return null;
            }
        };
    }

    WindowCounter newCounter(Ticker ticker);

    HitRatio newHitRatio(Ticker ticker);

    Top newTop(TopSettings settings, Ticker ticker);

    RollingHdrHistogram newHistogram(RecorderSettings settings, Ticker ticker);

}
