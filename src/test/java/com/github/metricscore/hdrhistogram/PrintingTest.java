package com.github.metricscore.hdrhistogram;

import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Snapshot;
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
                .withHighestTrackableValue(2, OverflowResolving.REDUCE_TO_MAXIMUM)
                .withLowestDiscernibleValue(1);
        System.out.println(builder.toString());
        System.out.println(builder.withSnapshotCachingDuration(Duration.ofDays(1)).toString());
        System.out.println(builder.withSnapshotCachingDuration(Duration.ZERO).toString());
        System.out.println(builder.withoutSnapshotOptimization().toString());
        System.out.println(builder.withPredefinedPercentiles(new double[] {0.5, 0.99}).toString());

        assertEquals(builder.toString(), builder.clone().toString());
    }

}
