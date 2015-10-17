package com.github.addon.metrics.health;

import com.codahale.metrics.health.HealthCheck;

public interface HealthChecked {

    String getName();

    HealthCheck.Result checkHealth() throws Exception;

    default HealthCheck getHealthCheck() {
        return new DefaultHealthCheck(this);
    }

}
