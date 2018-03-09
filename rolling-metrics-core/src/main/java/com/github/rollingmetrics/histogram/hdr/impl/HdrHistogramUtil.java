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

package com.github.rollingmetrics.histogram.hdr.impl;


import com.github.rollingmetrics.histogram.hdr.RollingSnapshot;
import com.github.rollingmetrics.histogram.hdr.impl.EmptyRollingSnapshot;
import org.HdrHistogram.AtomicHistogram;
import org.HdrHistogram.ConcurrentHistogram;
import org.HdrHistogram.Histogram;

import java.util.function.Function;

public class HdrHistogramUtil {

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

    public static RollingSnapshot getSnapshot(Histogram histogram, Function<Histogram, RollingSnapshot> snapshotTaker) {
        if (histogram.getTotalCount() > 0) {
            return snapshotTaker.apply(histogram);
        } else {
            return EmptyRollingSnapshot.INSTANCE;
        }
    }

    public static Histogram createNonConcurrentCopy(Histogram source) {
        if (source instanceof ConcurrentHistogram) {
            return new Histogram(source.getNumberOfSignificantValueDigits());
        } else if (source instanceof AtomicHistogram) {
            return new Histogram(
                    source.getLowestDiscernibleValue(),
                    source.getHighestTrackableValue(),
                    source.getNumberOfSignificantValueDigits()
            );
        } else {
            throw new IllegalArgumentException("Unsupported histogram class " + source.getClass());
        }
    }

}
