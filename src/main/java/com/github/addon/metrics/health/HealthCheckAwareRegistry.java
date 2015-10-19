package com.github.addon.metrics.health;

import com.codahale.metrics.health.HealthCheckRegistry;

import java.util.Collection;

public class HealthCheckAwareRegistry extends HealthCheckRegistry {

    public HealthCheckAwareRegistry(Collection<HealthChecked> observables) {
        super();
        for (HealthChecked observable: observables) {
            register(observable.getName(), observable.getHealthCheck());
        }
    }

}
