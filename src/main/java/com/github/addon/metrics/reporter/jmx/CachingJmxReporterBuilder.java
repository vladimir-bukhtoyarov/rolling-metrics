package com.github.addon.metrics.reporter.jmx;

import com.codahale.metrics.*;
import com.github.addon.metrics.reporter.MetricTimeUnits;

import javax.management.MBeanServer;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


/**
 * A builder for {@link JmxReporter} instances. Defaults to using the default MBean server and
 * not filtering metrics.
 */
public class CachingJmxReporterBuilder {
    private final MetricRegistry registry;
    private final Duration snapshotCachingDuration;
    private MBeanServer mBeanServer;
    private TimeUnit rateUnit;
    private TimeUnit durationUnit;
    private ObjectNameFactory objectNameFactory;
    private MetricFilter filter = MetricFilter.ALL;
    private String domain;
    private Map<String, TimeUnit> specificDurationUnits;
    private Map<String, TimeUnit> specificRateUnits;

    public CachingJmxReporterBuilder(MetricRegistry registry, Duration snapshotCachingDuration) {
        this.registry = Objects.requireNonNull(registry);
        this.snapshotCachingDuration = Objects.requireNonNull(snapshotCachingDuration);
        this.rateUnit = TimeUnit.SECONDS;
        this.durationUnit = TimeUnit.MILLISECONDS;
        this.domain = "metrics";
        this.objectNameFactory = new DefaultObjectNameFactory();
        this.specificDurationUnits = Collections.emptyMap();
        this.specificRateUnits = Collections.emptyMap();
    }

    /**
     * Register MBeans with the given {@link MBeanServer}.
     *
     * @param mBeanServer     an {@link MBeanServer}
     * @return {@code this}
     */
    public CachingJmxReporterBuilder registerWith(MBeanServer mBeanServer) {
        this.mBeanServer = mBeanServer;
        return this;
    }

    /**
     * Convert rates to the given time unit.
     *
     * @param rateUnit a unit of time
     * @return {@code this}
     */
    public CachingJmxReporterBuilder convertRatesTo(TimeUnit rateUnit) {
        this.rateUnit = rateUnit;
        return this;
    }

    public CachingJmxReporterBuilder createsObjectNamesWith(ObjectNameFactory onFactory) {
        if(onFactory == null) {
            throw new IllegalArgumentException("null objectNameFactory");
        }
        this.objectNameFactory = onFactory;
        return this;
    }

    /**
     * Convert durations to the given time unit.
     *
     * @param durationUnit a unit of time
     * @return {@code this}
     */
    public CachingJmxReporterBuilder convertDurationsTo(TimeUnit durationUnit) {
        this.durationUnit = durationUnit;
        return this;
    }

    /**
     * Only report metrics which match the given filter.
     *
     * @param filter a {@link MetricFilter}
     * @return {@code this}
     */
    public CachingJmxReporterBuilder filter(MetricFilter filter) {
        this.filter = filter;
        return this;
    }

    public CachingJmxReporterBuilder inDomain(String domain) {
        this.domain = domain;
        return this;
    }

    /**
     * Use specific {@link TimeUnit}s for the duration of the metrics with these names.
     *
     * @param specificDurationUnits a map of metric names and specific {@link TimeUnit}s
     * @return {@code this}
     */
    public CachingJmxReporterBuilder specificDurationUnits(Map<String, TimeUnit> specificDurationUnits) {
        this.specificDurationUnits = Collections.unmodifiableMap(specificDurationUnits);
        return this;
    }


    /**
     * Use specific {@link TimeUnit}s for the rate of the metrics with these names.
     *
     * @param specificRateUnits a map of metric names and specific {@link TimeUnit}s
     * @return {@code this}
     */
    public CachingJmxReporterBuilder specificRateUnits(Map<String, TimeUnit> specificRateUnits) {
        this.specificRateUnits = Collections.unmodifiableMap(specificRateUnits);
        return this;
    }

    /**
     * Builds a {@link JmxReporter} with the given properties.
     *
     * @return a {@link JmxReporter}
     */
    public CachingJmxReporter build() {
        final MetricTimeUnits timeUnits = new MetricTimeUnits(rateUnit, durationUnit, specificRateUnits, specificDurationUnits);
        if (mBeanServer==null) {
            mBeanServer = ManagementFactory.getPlatformMBeanServer();
        }
        return new CachingJmxReporter(mBeanServer, domain, registry, filter, timeUnits, objectNameFactory, snapshotCachingDuration);
    }

}


