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

import com.github.rollingmetrics.counter.WindowCounter;
import com.github.rollingmetrics.histogram.hdr.RollingHdrHistogramBuilder;
import com.github.rollingmetrics.hitratio.HitRatio;
import com.github.rollingmetrics.top.TopBuilder;
import com.github.rollingmetrics.util.Ticker;

import java.time.Duration;
import java.util.concurrent.Executor;

/**
 * TODO
 */
public interface RetentionPolicy {

    /**
     * TODO
     *
     * @return
     */
    WindowCounter newCounter();

    /**
     * TODO
     *
     * @return
     */
    HitRatio newHitRatio();

    /**
     * TODO
     *
     * @return
     */
    RollingHdrHistogramBuilder newRollingHdrHistogramBuilder();

    /**
     * Creates new instance of {@link TopBuilder}
     *
     * @param size maximum count of positions in the top
     *
     * @return new instance of {@link TopBuilder}
     */
    TopBuilder newTopBuilder(int size);

    /* Copied from hitratio TODO merge
     *
     * <p>
     * Concurrency properties:
     * <ul>
     *     <li>Writing is lock-free. Writers do not block writers and readers.</li>
     *     <li>Reading is lock-free. Readers do not block writers and readers.</li>
     * </ul>
     *
     * <p>
     * Usage recommendations:
     * <ul>
     *     <li>When you do not need in "rolling time window" semantic. Else use {@link SmoothlyDecayingRollingHitRatio}</li>
     *     <li>Normally you should not use this implementation because in real world use-cases you need to show measurements which actual to current moment of time or time window.</li>
     * </ul>
     */
    static DefaultRetentionPolicy uniform() {
        return new UniformRetentionPolicy();
    }

    /* Copied from counter. TODO merge
     *
     * <p>
     * Concurrency properties:
     * <ul>
     *     <li>Writing is lock-free. Writers do not block writers and readers.</li>
     *     <li>Sum reading always happen inside synchronized block, so readers block each other, but readers never block writers.</li>
     * </ul>
     *
     * <p>
     * Usage recommendations:
     * <ul>
     *     <li>When you do not need in "rolling time window" semantic. Else use {@link SmoothlyDecayingRollingCounter}</li>
     *     <li>When you need in 100 percents guarantee that one measure can not be reported twice.</li>
     *     <li>Only if one kind of reader interests in value of counter.
     *     Usage of this implementation for case of multiple readers will be a bad idea because of readers will steal data from each other.
     *     </li>
     * </ul>
     *  <p>
     * Usage recommendations:
     * <ul>
     *     <li>When you do not need in "rolling time window" semantic. Else use {@link TODO}</li>
     *     <li>When you want to limit time which each increment takes affect to counter sum in order to avoid reporting of obsolete measurements.</li>
     *     <li>Only if you accept the fact that several increments can be never observed by reader(because rotation to zero can happen before reader seen the written values).</li>
     * </ul>
     *
     * @return
     */
    /*  Copied from hitratio TODO merge
     * <p>
     * Concurrency properties:
     * <ul>
     *     <li>Writing is lock-free. Writers do not block writers and readers.</li>
     *     <li>Reading is lock-free. Readers do not block writers and readers.</li>
     * </ul>
     *
     * <p>
     * Usage recommendations:
     * <ul>
     *     <li>When you do not need in "rolling time window" semantic. Else use {@link SmoothlyDecayingRollingHitRatio}</li>
     *     <li>When you need in 100 percents guarantee that one measure can not be reported twice.</li>
     *     <li>Only if one kind of reader interests in value of hit-ratio.
     *     Usage of this implementation for case of multiple readers will be a bad idea because of readers will steal data from each other.
     *     </li>
     * </ul>
     * @return
     */
    static DefaultRetentionPolicy resetOnSnapshot() {
        return new ResetOnSnapshotRetentionPolicy();
    }

    /* Copied from counter. TODO merge
     *
     * <p>
     * Concurrency properties:
     * <ul>
     *     <li>Writing is lock-free.</li>
     *     <li>Sum reading is lock-free.</li>
     * </ul>
     *
     * @param resettingPeriod
     * @return
     */

    /* Copied from hitratio TODO merge
     *
     * <p>
     * Concurrency properties:
     * <ul>
     *     <li>Writing is lock-free.</li>
     *     <li>Ratio calculation is lock-free.</li>
     * </ul>
     *
     * <p>
     * Usage recommendations:
     * <ul>
     *     <li>When you do not need in "rolling time window" semantic. Else use {@link SmoothlyDecayingRollingHitRatio}</li>
     *     <li>When you want to limit time which each increment takes affect to hit-ratio in order to avoid reporting of obsolete measurements.</li>
     *     <li>Only if you accept the fact that several increments can be never observed by reader(because rotation to zero can happen before reader seen the written values).</li>
     * </ul>
     * @param resettingPeriod
     * @return
     */
    static DefaultRetentionPolicy resetPeriodically(Duration resettingPeriod) {
        return new ResetPeriodicallyRetentionPolicy(resettingPeriod);
    }

    /*
     * Copied from counter. TODO merge
     *
     * The unique properties which makes this counter probably the best "rolling time window" implementation are following:
     * <ul>
     *     <li>Sufficient performance about tens of millions concurrent writes and reads per second.</li>
     *     <li>Predictable and low memory consumption, the memory which consumed by counter does not depend from amount and frequency of writes.</li>
     *     <li>Perfectly user experience, the continuous observation does not see the sudden changes of sum.
     *     This property achieved by smoothly decaying of oldest chunk of counter.
     *     </li>
     * </ul>
     *
     * <p>
     * Concurrency properties:
     * <ul>
     *     <li>Writing is lock-free.
     *     <li>Sum reading is lock-free.
     * </ul>
     *
     * <p>
     * Usage recommendations:
     * <ul>
     *     <li>Only when you need in "rolling time window" semantic.</li>
     * </ul>
     *
     * <p>
     * Performance considerations:
     * <ul>
     *     <li>You can consider writing speed as a constant. The write latency does not depend from count of chunk or frequency of chunk rotation.
     *     <li>The writing depends only from level of contention between writers(internally counter implemented across AtomicLong).</li>
     *     <li>The huge count of chunk leads to the slower calculation of their sum. So precision of sum conflicts with latency of sum. You need to choose meaningful values.
     *     For example 10 chunks will guarantee at least 90% accuracy and ten million reads per second.</li>
     * </ul>
     *
     * <p> Example of usage:
     * <pre><code>
     *         // constructs the counter which divided by 10 chunks with 60 seconds time window.
     *         // one chunk will be reset to zero after each 6 second,
     *         WindowCounter counter = new SmoothlyDecayingRollingCounter(Duration.ofSeconds(60), 10);
     *         counter.add(42);
     *     </code>
     * </pre>
     * @param rollingTimeWindow
     * @param numberChunks
     * @return
     */

    /* Copied from hitratio TODO merge
     * The unique properties which makes this hit-ratio probably the best "rolling time window" implementation are following:
     * <ul>
     *     <li>Sufficient performance about tens of millions concurrent writes and reads per second.</li>
     *     <li>Predictable and low memory consumption, the memory which consumed by hit-ratio does not depend from amount and frequency of writes.</li>
     *     <li>Perfectly user experience, the continuous observation does not see the sudden changes of sum.
     *     This property achieved by smoothly decaying of oldest chunk of hit-ratio.
     *     </li>
     * </ul>
     *
     * <p>
     * Concurrency properties:
     * <ul>
     *     <li>Writing is lock-free.
     *     <li>Ratio calculation is lock-free.
     * </ul>
     *
     * <p>
     * Usage recommendations:
     * <ul>
     *     <li>Only when you need in "rolling time window" semantic.</li>
     * </ul>
     *
     * <p>
     * Performance considerations:
     * <ul>
     *     <li>You can consider writing speed as a constant. The write latency does not depend from count of chunk or frequency of chunk rotation.
     *     <li>The writing depends only from level of contention between writers(internally hit-ratio implemented across AtomicLong).</li>
     *     <li>The huge count of chunk leads to the slower calculation of their ratio. So precision of getHitRatio conflicts with latency of getHitRatio. You need to choose meaningful values.
     *     For example 10 chunks will guarantee at least 90% accuracy and ten million reads per second.</li>
     * </ul>
     * @param rollingTimeWindow
     * @param numberChunks
     * @return
     */
    static DefaultRetentionPolicy resetPeriodicallyByChunks(Duration rollingTimeWindow, int numberChunks) {
        return new ResetPeriodicallyByChunksRetentionPolicy(numberChunks, rollingTimeWindow);
    }

    /**
     * TODO
     *
     * @return
     */
    Executor getExecutor();

    /**
     * TODO
     *
     * @return
     */
    Ticker getTicker();

    /**
     * TODO
     *
     * @return
     */
    Duration getSnapshotCachingDuration();

}