package com.onefinancial.customer.autoconfigure;

import com.onefinancial.customer.core.spi.CustomerOperationMetrics;

import java.time.Duration;

/**
 * No-op implementation of {@link CustomerOperationMetrics}.
 *
 * <p>Registered as the default fallback when no observability module or host-provided
 * implementation is on the classpath.</p>
 */
final class NoOpOperationMetrics implements CustomerOperationMetrics {

    @Override
    public void recordOperation(String operation, String status, Duration duration) {
        // intentionally empty — metrics are disabled
    }
}
