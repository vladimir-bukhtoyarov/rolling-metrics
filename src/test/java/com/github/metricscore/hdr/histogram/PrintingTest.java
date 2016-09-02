/*
 *
 *  Copyright 2016 Vladimir Bukhtoyarov
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

package com.github.metricscore.hdr.histogram;

import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Snapshot;
import com.github.metricscore.hdr.histogram.HdrBuilder;
import com.github.metricscore.hdr.histogram.OverflowResolver;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.function.Function;

import static junit.framework.TestCase.assertEquals;

public class PrintingTest {

    private Function<Reservoir, Snapshot> snapshotTaker = reservoir -> {
        for (int i = 1; i <= 1000; i++) {
            reservoir.update(i);
        }
        return reservoir.getSnapshot();
    };

    @Test
    public void testSmartSnapshotPrinting() {
        Reservoir reservoir = new HdrBuilder().buildReservoir();
        Snapshot snapshot = snapshotTaker.apply(reservoir);

        System.out.println(snapshot);
        snapshot.dump(new ByteArrayOutputStream());
    }

    @Test
    public void testFullSnapshotPrinting() {
        Reservoir reservoir = new HdrBuilder().withoutSnapshotOptimization().buildReservoir();
        Snapshot snapshot = snapshotTaker.apply(reservoir);

        System.out.println(snapshot);
        snapshot.dump(new ByteArrayOutputStream());
    }

    @Test
    public void testBuilderPrinting() {
        HdrBuilder builder = new HdrBuilder()
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
