package com.onefinancial.customer.core.spi;

import java.time.Duration;

/**
 * SPI for recording customer operation metrics.
 *
 * <p>Implementations are provided by the observability module. The core service
 * uses this port to avoid a direct dependency on infrastructure (Micrometer).</p>
 */
public interface CustomerOperationMetrics {

    /**
     * Records a customer operation with its outcome and duration.
     *
     * @param operation the operation name (e.g., "create", "update", "delete", "status_change")
     * @param status    the outcome status ("success" or "error")
     * @param duration  the elapsed duration
     */
    void recordOperation(String operation, String status, Duration duration);
}
