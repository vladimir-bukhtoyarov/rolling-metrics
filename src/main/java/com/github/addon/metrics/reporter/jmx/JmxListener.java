package com.github.addon.metrics.reporter.jmx;

import com.codahale.metrics.*;
import com.github.addon.metrics.reporter.MetricTimeUnits;

import javax.management.*;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JmxListener implements MetricRegistryListener {
    private final String name;
    private final MBeanServer mBeanServer;
    private final MetricFilter filter;
    private final MetricTimeUnits timeUnits;
    private final Map<ObjectName, ObjectName> registered;
    private final ObjectNameFactory objectNameFactory;
    private final Duration cachingDuration;

    public JmxListener(MBeanServer mBeanServer, String name, MetricFilter filter, MetricTimeUnits timeUnits, ObjectNameFactory objectNameFactory, Duration cachingDuration) {
        this.mBeanServer = mBeanServer;
        this.name = name;
        this.filter = filter;
        this.timeUnits = timeUnits;
        this.registered = new ConcurrentHashMap<ObjectName, ObjectName>();
        this.objectNameFactory = objectNameFactory;
        this.cachingDuration = cachingDuration;
    }

    @Override
    public void onGaugeAdded(String name, Gauge<?> gauge) {
        try {
            if (filter.matches(name, gauge)) {
                final ObjectName objectName = createName("gauges", name);
                registerMBean(new JmxGauge(gauge, objectName, cachingDuration), objectName);
            }
        } catch (InstanceAlreadyExistsException e) {
            CachingJmxReporter.LOGGER.debug("Unable to register gauge", e);
        } catch (JMException e) {
            CachingJmxReporter.LOGGER.warn("Unable to register gauge", e);
        }
    }

    @Override
    public void onGaugeRemoved(String name) {
        try {
            final ObjectName objectName = createName("gauges", name);
            unregisterMBean(objectName);
        } catch (InstanceNotFoundException e) {
            CachingJmxReporter.LOGGER.debug("Unable to unregister gauge", e);
        } catch (MBeanRegistrationException e) {
            CachingJmxReporter.LOGGER.warn("Unable to unregister gauge", e);
        }
    }

    @Override
    public void onCounterAdded(String name, Counter counter) {
        try {
            if (filter.matches(name, counter)) {
                final ObjectName objectName = createName("counters", name);
                registerMBean(new JmxCounter(counter, objectName, cachingDuration), objectName);
            }
        } catch (InstanceAlreadyExistsException e) {
            CachingJmxReporter.LOGGER.debug("Unable to register counter", e);
        } catch (JMException e) {
            CachingJmxReporter.LOGGER.warn("Unable to register counter", e);
        }
    }

    @Override
    public void onCounterRemoved(String name) {
        try {
            final ObjectName objectName = createName("counters", name);
            unregisterMBean(objectName);
        } catch (InstanceNotFoundException e) {
            CachingJmxReporter.LOGGER.debug("Unable to unregister counter", e);
        } catch (MBeanRegistrationException e) {
            CachingJmxReporter.LOGGER.warn("Unable to unregister counter", e);
        }
    }

    @Override
    public void onHistogramAdded(String name, Histogram histogram) {
        try {
            if (filter.matches(name, histogram)) {
                final ObjectName objectName = createName("histograms", name);
                registerMBean(new JmxHistogram(histogram, objectName, cachingDuration), objectName);
            }
        } catch (InstanceAlreadyExistsException e) {
            CachingJmxReporter.LOGGER.debug("Unable to register histogram", e);
        } catch (JMException e) {
            CachingJmxReporter.LOGGER.warn("Unable to register histogram", e);
        }
    }

    @Override
    public void onHistogramRemoved(String name) {
        try {
            final ObjectName objectName = createName("histograms", name);
            unregisterMBean(objectName);
        } catch (InstanceNotFoundException e) {
            CachingJmxReporter.LOGGER.debug("Unable to unregister histogram", e);
        } catch (MBeanRegistrationException e) {
            CachingJmxReporter.LOGGER.warn("Unable to unregister histogram", e);
        }
    }

    @Override
    public void onMeterAdded(String name, Meter meter) {
        try {
            if (filter.matches(name, meter)) {
                final ObjectName objectName = createName("meters", name);
                registerMBean(new JmxMeter(meter, objectName, timeUnits.rateFor(name), cachingDuration), objectName);
            }
        } catch (InstanceAlreadyExistsException e) {
            CachingJmxReporter.LOGGER.debug("Unable to register meter", e);
        } catch (JMException e) {
            CachingJmxReporter.LOGGER.warn("Unable to register meter", e);
        }
    }

    @Override
    public void onMeterRemoved(String name) {
        try {
            final ObjectName objectName = createName("meters", name);
            unregisterMBean(objectName);
        } catch (InstanceNotFoundException e) {
            CachingJmxReporter.LOGGER.debug("Unable to unregister meter", e);
        } catch (MBeanRegistrationException e) {
            CachingJmxReporter.LOGGER.warn("Unable to unregister meter", e);
        }
    }

    @Override
    public void onTimerAdded(String name, Timer timer) {
        try {
            if (filter.matches(name, timer)) {
                final ObjectName objectName = createName("timers", name);
                registerMBean(new JmxTimer(timer, objectName, timeUnits.rateFor(name), timeUnits.durationFor(name), cachingDuration), objectName);
            }
        } catch (InstanceAlreadyExistsException e) {
            CachingJmxReporter.LOGGER.debug("Unable to register timer", e);
        } catch (JMException e) {
            CachingJmxReporter.LOGGER.warn("Unable to register timer", e);
        }
    }

    @Override
    public void onTimerRemoved(String name) {
        try {
            final ObjectName objectName = createName("timers", name);
            unregisterMBean(objectName);
        } catch (InstanceNotFoundException e) {
            CachingJmxReporter.LOGGER.debug("Unable to unregister timer", e);
        } catch (MBeanRegistrationException e) {
            CachingJmxReporter.LOGGER.warn("Unable to unregister timer", e);
        }
    }

    private void registerMBean(Object mBean, ObjectName objectName) throws InstanceAlreadyExistsException, JMException {
        ObjectInstance objectInstance = mBeanServer.registerMBean(mBean, objectName);
        if (objectInstance != null) {
            // the websphere mbeanserver rewrites the objectname to include
            // cell, node & server info
            // make sure we capture the new objectName for unregistration
            registered.put(objectName, objectInstance.getObjectName());
        } else {
            registered.put(objectName, objectName);
        }
    }

    private void unregisterMBean(ObjectName originalObjectName) throws InstanceNotFoundException, MBeanRegistrationException {
        ObjectName storedObjectName = registered.remove(originalObjectName);
        if (storedObjectName != null) {
            mBeanServer.unregisterMBean(storedObjectName);
        } else {
            mBeanServer.unregisterMBean(originalObjectName);
        }
    }

    private ObjectName createName(String type, String name) {
        return objectNameFactory.createName(type, this.name, name);
    }

    void unregisterAll() {
        for (ObjectName name : registered.keySet()) {
            try {
                unregisterMBean(name);
            } catch (InstanceNotFoundException e) {
                CachingJmxReporter.LOGGER.debug("Unable to unregister metric", e);
            } catch (MBeanRegistrationException e) {
                CachingJmxReporter.LOGGER.warn("Unable to unregister metric", e);
            }
        }
        registered.clear();
    }

    public ObjectNameFactory getObjectNameFactory() {
        return objectNameFactory;
    }

}
