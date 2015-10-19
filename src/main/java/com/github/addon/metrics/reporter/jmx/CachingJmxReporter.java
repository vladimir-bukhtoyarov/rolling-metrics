package com.github.addon.metrics.reporter.jmx;

import com.codahale.metrics.*;
import com.github.addon.metrics.reporter.MetricTimeUnits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import java.io.Closeable;
import java.time.Duration;

public class CachingJmxReporter implements Reporter, Closeable {

    public static final Logger LOGGER = LoggerFactory.getLogger(JmxReporter.class);

    /**
     * Returns a new {@link CachingJmxReporterBuilder} for {@link JmxReporter}.
     *
     * @param registry the registry to report
     * @param snapshotCachingDuration how long snapshots should be cached
     * @return a {@link CachingJmxReporterBuilder} instance for a {@link JmxReporter}
     */
    public static CachingJmxReporterBuilder forRegistry(MetricRegistry registry, Duration snapshotCachingDuration) {
        return new CachingJmxReporterBuilder(registry, snapshotCachingDuration);
    }

    private final MetricRegistry registry;
    private final JmxListener listener;
    private final Duration snapshotCanchingDuration;

    public CachingJmxReporter(MBeanServer mBeanServer,
                              String domain,
                              MetricRegistry registry,
                              MetricFilter filter,
                              MetricTimeUnits timeUnits,
                              ObjectNameFactory objectNameFactory,
                              Duration canchingDuration) {
        this.registry = registry;
        this.listener = new JmxListener(mBeanServer, domain, filter, timeUnits, objectNameFactory, canchingDuration);
        this.snapshotCanchingDuration = canchingDuration;
    }

    /**
     * Starts the reporter.
     */
    public void start() {
        registry.addListener(listener);
    }

    /**
     * Stops the reporter.
     */
    public void stop() {
        registry.removeListener(listener);
        listener.unregisterAll();
    }

    /**
     * Stops the reporter.
     */
    @Override
    public void close() {
        stop();
    }

    /**
     * Visible for testing
     */
    ObjectNameFactory getObjectNameFactory() {
        return listener.getObjectNameFactory();
    }

}
