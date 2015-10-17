package com.github.addon.metrics.servlet;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.servlets.MetricsServlet;

import java.util.Objects;

public class RegistryAwareMetricsContextListener extends MetricsServlet.ContextListener {

    private final MetricRegistry registry;

    public RegistryAwareMetricsContextListener(MetricRegistry registry) {
        this.registry = Objects.requireNonNull(registry);
    }

    public RegistryAwareMetricsContextListener() {
        this.registry = new MetricRegistry();
    }

    @Override
    protected MetricRegistry getMetricRegistry() {
        return registry;
    }

}
