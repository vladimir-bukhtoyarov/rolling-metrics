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

package com.github.rollingmetrics.histogram.util;


import com.codahale.metrics.Snapshot;
import org.HdrHistogram.AtomicHistogram;
import org.HdrHistogram.ConcurrentHistogram;
import org.HdrHistogram.Histogram;

import java.util.function.Function;

public class HistogramUtil {

    public static void reset(Histogram histogram) {
        if (histogram.getTotalCount() > 0) {
            histogram.reset();
        }
    }

    public static void addSecondToFirst(Histogram first, Histogram second) {
        if (second.getTotalCount() > 0) {
            first.add(second);
        }
    }

    public static Snapshot getSnapshot(Histogram histogram, Function<Histogram, Snapshot> snapshotTaker) {
        if (histogram.getTotalCount() > 0) {
            return snapshotTaker.apply(histogram);
        } else {
            return EmptySnapshot.INSTANCE;
        }
    }

    public static Histogram createNonConcurrentCopy(Histogram source) {
        if (source instanceof ConcurrentHistogram) {
            return new Histogram(source.getNumberOfSignificantValueDigits());
        } else if (source instanceof AtomicHistogram) {
            return new AtomicHistogram(
                    source.getLowestDiscernibleValue(),
                    source.getHighestTrackableValue(),
                    source.getNumberOfSignificantValueDigits()
            );
        } else {
            throw new IllegalArgumentException("Unsupported histogram class " + source.getClass());
        }
    }

}
