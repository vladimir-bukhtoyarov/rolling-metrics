package com.github.addon.metrics.health;

import com.codahale.metrics.health.HealthCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultHealthCheck extends HealthCheck {

    static final Logger logger = LoggerFactory.getLogger(DefaultHealthCheck.class);

    private final HealthChecked observable;

    public DefaultHealthCheck(HealthChecked observable) {
        this.observable = observable;
    }

    @Override
    protected Result check() throws Exception {
        final String serviceName = observable.getName();
        logger.info("Beginning health check of [{}]", serviceName);
        try {
            HealthCheck.Result status = observable.checkHealth();
            final String message = status.getMessage();
            if (status.isHealthy()) {
                logger.info("Successfully verified health of [{}] with result [{}]", serviceName, message);
                return Result.healthy(message);
            } else {
                logger.error("Unsuccessfully verified health of [{}] with result [{}]", serviceName, message);
                return Result.unhealthy(message);
            }
        } catch (Exception e) {
            String writableReason = e.getMessage() != null? e.getMessage(): e.getClass().getName();
            logger.error("Fail to verify health of " + serviceName + " cause " + writableReason, e);
            return Result.unhealthy(writableReason);
        }
    }

}
