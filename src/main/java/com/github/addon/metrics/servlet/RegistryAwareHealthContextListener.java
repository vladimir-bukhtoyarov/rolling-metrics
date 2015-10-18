package com.github.addon.metrics.servlet;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.servlets.HealthCheckServlet;

import java.util.Objects;

public class RegistryAwareHealthContextListener extends HealthCheckServlet.ContextListener {

    private final HealthCheckRegistry registry;

    public RegistryAwareHealthContextListener(HealthCheckRegistry registry) {
        this.registry = Objects.requireNonNull(registry);
    }

    public RegistryAwareHealthContextListener() {
        this.registry = new HealthCheckRegistry();
    }

    @Override
    protected HealthCheckRegistry getHealthCheckRegistry() {
        return registry;
    }

}
