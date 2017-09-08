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

package com.github.rollingmetrics.histogram.hdr;

import com.github.rollingmetrics.histogram.OverflowResolver;
import org.junit.Test;
import java.time.Duration;
import java.util.function.Function;

import static junit.framework.TestCase.assertEquals;

public class PrintingTest {

    private Function<RollingHdrHistogram, RollingSnapshot> snapshotTaker = reservoir -> {
        for (int i = 1; i <= 1000; i++) {
            reservoir.update(i);
        }
        return reservoir.getSnapshot();
    };

    @Test
    public void testSmartSnapshotPrinting() {
        RollingHdrHistogram histogram = RollingHdrHistogram.builder().build();
        RollingSnapshot snapshot = snapshotTaker.apply(histogram);
        System.out.println(snapshot);
    }

    @Test
    public void testFullSnapshotPrinting() {
        RollingHdrHistogram histogram = RollingHdrHistogram.builder()
                .withoutSnapshotOptimization().build();
        RollingSnapshot snapshot = snapshotTaker.apply(histogram);
        System.out.println(snapshot);
    }

    @Test
    public void testBuilderPrinting() {
        RollingHdrHistogramBuilder builder = RollingHdrHistogram.builder()
                .withHighestTrackableValue(2, OverflowResolver.REDUCE_TO_HIGHEST_TRACKABLE)
                .withLowestDiscernibleValue(1);
        System.out.println(builder.toString());
        System.out.println(builder.withSnapshotCachingDuration(Duration.ofDays(1)).toString());
        System.out.println(builder.withSnapshotCachingDuration(Duration.ZERO).toString());
        System.out.println(builder.withoutSnapshotOptimization().toString());
        System.out.println(builder.withPredefinedPercentiles(new double[] {0.5, 0.99}).toString());

        assertEquals(builder.toString(), builder.deepCopy().toString());
    }

}
