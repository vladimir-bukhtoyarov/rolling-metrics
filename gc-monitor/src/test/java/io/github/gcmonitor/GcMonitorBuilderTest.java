/*
 *  Copyright 2017 Vladimir Bukhtoyarov
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.github.gcmonitor;

import com.github.rollingmetrics.util.NamingUtils;
import com.github.rollingmetrics.util.Ticker;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;

import static org.junit.Assert.*;

@RunWith(Enclosed.class)
public class GcMonitorBuilderTest {

    private static abstract class GcMonitorBuilderTestBase {

        GcMonitorBuilder builder = GcMonitor.builder();
    }

    public static class PercentileSettings extends GcMonitorBuilderTestBase {

        @Test(expected = IllegalArgumentException.class)
        public void shouldDisallowNegativePercentile() {
            builder.withPercentiles(new double[] {0.5, -0.01});
        }

        @Test(expected = IllegalArgumentException.class)
        public void shouldDisallowPercentileGreateThan1() {
            builder.withPercentiles(new double[] {0.5, 1.01});
        }

        @Test(expected = IllegalArgumentException.class)
        public void shouldDisallowDuplicates() {
            builder.withPercentiles(new double[] {0.5, 0.7, 0.5});
        }

        @Test(expected = IllegalArgumentException.class)
        public void shouldDisallowEmptyPercentiles() {
            builder.withPercentiles(new double[0]);
        }

        @Test(expected = IllegalArgumentException.class)
        public void shouldDisallowNullPercentiles() {
            builder.withPercentiles(null);
        }

        @Test
        public void shouldSuccessfullyApplyCustomPercentiles() {
            double[] customPercentiles = {0.25, 0.5, 0.9, 0.999};
            builder.withPercentiles(customPercentiles);
            GcMonitor monitor = builder.build();
            assertArrayEquals(customPercentiles, monitor.getConfiguration().getPercentiles(), 0.0d);
        }

    }

    public static class RollingWindowSettings extends GcMonitorBuilderTestBase {

        @Test(expected = IllegalArgumentException.class)
        public void shouldDisallowNegativeWindow() {
            builder.addRollingWindow("my-window", Duration.ofMinutes(-1));
        }

        @Test(expected = IllegalArgumentException.class)
        public void shouldDisallowZeroWindow() {
            builder.addRollingWindow("my-window", Duration.ofMinutes(0));
        }

        @Test(expected = IllegalArgumentException.class)
        public void shouldDisallowNullWindow() {
            builder.addRollingWindow("my-window", null);
        }

        @Test(expected = IllegalArgumentException.class)
        public void shouldDisallowNullWindowName() {
            builder.addRollingWindow(null, Duration.ofMinutes(1));
        }

        @Test(expected = IllegalArgumentException.class)
        public void shouldDisallowDuplicatesOfWindowName() {
            builder.addRollingWindow("my-window", Duration.ofMinutes(1));
            builder.addRollingWindow("my-window", Duration.ofMinutes(90));
        }

        @Test
        public void uniformWindowShouldBeEnabledByDefault() {
            GcMonitor monitor = builder.build();
            Set<String> windows = monitor.getConfiguration().getWindowNames();
            assertTrue(windows.contains(GcMonitorConfiguration.UNIFORM_WINDOW_NAME));
        }

        @Test
        public void testDisableUniformWindow() {
            GcMonitor monitor = builder.withoutUniformWindow().build();
            Set<String> windows = monitor.getConfiguration().getWindowNames();
            assertFalse(windows.contains(GcMonitorConfiguration.UNIFORM_WINDOW_NAME));
        }

    }

    public static class CollectorsSettingsTest extends GcMonitorBuilderTestBase {

        @Test
        public void allCollectorsFromJvmShouldBeUsedByDefault() {
            GcMonitor monitor = builder.build();
            for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
                SortedSet<String> collectorNames = monitor.getConfiguration().getCollectorNames();
                String name = NamingUtils.replaceAllWhitespaces(bean.getName());
                assertTrue(collectorNames.contains(name));
            }
        }

        @Test
        public void aggregationShouldBeEnabledByDefault() {
            GarbageCollectorMXBeanMock mock1 = new GarbageCollectorMXBeanMock("terminator-1");
            GarbageCollectorMXBeanMock mock2 = new GarbageCollectorMXBeanMock("terminator-2");
            GcMonitor monitor = GcMonitor.builder(Arrays.asList(mock1, mock2)).build();
            assertTrue(monitor.getConfiguration().getCollectorNames().contains(GcMonitorConfiguration.AGGREGATED_COLLECTOR_NAME));
        }

        @Test
        public void testDisableAggregation() {
            GarbageCollectorMXBeanMock mock1 = new GarbageCollectorMXBeanMock("terminator-1");
            GarbageCollectorMXBeanMock mock2 = new GarbageCollectorMXBeanMock("terminator-2");
            GcMonitor monitor = GcMonitor.builder(Arrays.asList(mock1, mock2))
                    .withoutCollectorsAggregation()
                    .build();
            assertFalse(monitor.getConfiguration().getCollectorNames().contains(GcMonitorConfiguration.AGGREGATED_COLLECTOR_NAME));
        }

        @Test
        public void aggregationShouldBeDisabledInCaseOfSingleCollector() {
            GarbageCollectorMXBeanMock mock = new GarbageCollectorMXBeanMock("terminator");
            GcMonitor monitor = GcMonitor.builder(Arrays.asList(mock)).build();
            SortedSet<String> collectorNames = monitor.getConfiguration().getCollectorNames();
            assertEquals(collectorNames, Collections.singleton("terminator"));
        }

        @Test(expected = IllegalArgumentException.class)
        public void multipleCollectorsWithSameNameShouldBeDisabled() {
            GarbageCollectorMXBeanMock mock1 = new GarbageCollectorMXBeanMock("terminator");
            GarbageCollectorMXBeanMock mock2 = new GarbageCollectorMXBeanMock("terminator");
            GcMonitor.builder(Arrays.asList(mock1, mock2)).build();
        }

        @Test(expected = IllegalArgumentException.class)
        public void nullCollectorShouldBeDisabled() {
            GarbageCollectorMXBeanMock mock1 = new GarbageCollectorMXBeanMock("terminator");
            GarbageCollectorMXBeanMock mock2 = null;
            GcMonitor.builder(Arrays.asList(mock1, mock2)).build();
        }

        @Test(expected = IllegalArgumentException.class)
        public void emptyCollectorCollectionShouldBeDisabled() {
            GcMonitor.builder(Collections.emptyList());
        }

        @Test(expected = IllegalArgumentException.class)
        public void nullCollectorCollectionShouldBeDisabled() {
            GcMonitor.builder(null);
        }

    }

    public static class TickerSettingsTest extends GcMonitorBuilderTestBase {

        @Test(expected = IllegalArgumentException.class)
        public void nullTickerShouldBeDeprecated() {
            builder.withTicker(null);
        }

        @Test
        public void applyTickerSettingsToMonitor() {
            Ticker ticker = new Ticker() {
                @Override
                public long nanoTime() {
                    return System.nanoTime();
                }

                @Override
                public long stableMilliseconds() {
                    return System.currentTimeMillis();
                }
            };
            builder.withTicker(ticker);
            GcMonitor monitor = builder.build();
            assertSame(ticker, monitor.getConfiguration().getTicker());
        }

    }

}