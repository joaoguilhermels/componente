package com.onefinancial.customer.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import com.onefinancial.customer.core.spi.CustomerOperationMetrics;

import java.time.Duration;

/**
 * Facade for Customer Registry Micrometer metrics.
 *
 * <p>All metrics are prefixed with {@code customer_registry_} and use
 * only low-cardinality tags to prevent metric explosion.</p>
 *
 * <h3>Registered metrics:</h3>
 * <ul>
 *   <li>{@code customer_registry_operations_total} — counter by operation + status</li>
 *   <li>{@code customer_registry_operation_duration_seconds} — timer by operation</li>
 *   <li>{@code customer_registry_schema_migration_total} — counter by from/to version + status</li>
 *   <li>{@code customer_registry_schema_migration_duration_seconds} — timer by from/to version</li>
 *   <li>{@code customer_registry_schema_migration_lock_contention_total} — counter</li>
 * </ul>
 */
public class CustomerRegistryMetrics implements CustomerOperationMetrics {

    private static final String PREFIX = "customer_registry";

    private final MeterRegistry registry;

    public CustomerRegistryMetrics(MeterRegistry registry) {
        this.registry = registry;

        // Pre-register lock contention counter
        Counter.builder(PREFIX + "_schema_migration_lock_contention_total")
            .description("Number of times schema migration lock acquisition was contended")
            .register(registry);
    }

    /**
     * Records a customer operation (create, update, delete, status_change).
     */
    @Override
    public void recordOperation(String operation, String status, Duration duration) {
        Counter.builder(PREFIX + "_operations_total")
            .tag("operation", operation)
            .tag("status", status)
            .description("Total customer registry operations")
            .register(registry)
            .increment();

        Timer.builder(PREFIX + "_operation_duration_seconds")
            .tag("operation", operation)
            .description("Duration of customer registry operations")
            .register(registry)
            .record(duration);
    }

    /**
     * Records a schema migration execution.
     */
    public void recordMigration(int fromVersion, int toVersion, String status, Duration duration) {
        Counter.builder(PREFIX + "_schema_migration_total")
            .tag("from", String.valueOf(fromVersion))
            .tag("to", String.valueOf(toVersion))
            .tag("status", status)
            .description("Total schema migrations executed")
            .register(registry)
            .increment();

        Timer.builder(PREFIX + "_schema_migration_duration_seconds")
            .tag("from", String.valueOf(fromVersion))
            .tag("to", String.valueOf(toVersion))
            .description("Duration of schema migrations")
            .register(registry)
            .record(duration);
    }

    /**
     * Increments the lock contention counter.
     */
    public void recordLockContention() {
        registry.counter(PREFIX + "_schema_migration_lock_contention_total")
            .increment();
    }

    /**
     * Returns the underlying registry for advanced use cases.
     */
    public MeterRegistry getRegistry() {
        return registry;
    }
}
