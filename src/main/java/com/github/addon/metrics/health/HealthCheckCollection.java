package com.github.addon.metrics.health;

import com.codahale.metrics.health.HealthCheckRegistry;

import java.util.Collection;

public class HealthCheckCollection extends HealthCheckRegistry {

    public HealthCheckCollection(Collection<HealthChecked> observables) {
        super();
        for (HealthChecked observable: observables) {
            register(observable.getName(), observable.getHealthCheck());
        }
    }

}
