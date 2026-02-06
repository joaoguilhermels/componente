package com.oneff.customer.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerRegistryMetricsTest {

    private MeterRegistry registry;
    private CustomerRegistryMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new CustomerRegistryMetrics(registry);
    }

    @Test
    @DisplayName("should record operation counter with tags")
    void recordOperationCounter() {
        metrics.recordOperation("create", "success", Duration.ofMillis(50));

        double count = registry.counter(
            "customer_registry_operations_total",
            "operation", "create", "status", "success"
        ).count();
        assertThat(count).isEqualTo(1.0);
    }

    @Test
    @DisplayName("should record operation timer with duration")
    void recordOperationTimer() {
        metrics.recordOperation("create", "success", Duration.ofMillis(150));

        long timerCount = registry.timer(
            "customer_registry_operation_duration_seconds",
            "operation", "create"
        ).count();
        assertThat(timerCount).isEqualTo(1);
    }

    @Test
    @DisplayName("should record multiple operations independently")
    void recordMultipleOperations() {
        metrics.recordOperation("create", "success", Duration.ofMillis(50));
        metrics.recordOperation("create", "failure", Duration.ofMillis(30));
        metrics.recordOperation("update", "success", Duration.ofMillis(40));

        assertThat(registry.counter(
            "customer_registry_operations_total",
            "operation", "create", "status", "success"
        ).count()).isEqualTo(1.0);

        assertThat(registry.counter(
            "customer_registry_operations_total",
            "operation", "create", "status", "failure"
        ).count()).isEqualTo(1.0);

        assertThat(registry.counter(
            "customer_registry_operations_total",
            "operation", "update", "status", "success"
        ).count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("should record migration counter with version tags")
    void recordMigrationCounter() {
        metrics.recordMigration(1, 2, "success", Duration.ofSeconds(2));

        double count = registry.counter(
            "customer_registry_schema_migration_total",
            "from", "1", "to", "2", "status", "success"
        ).count();
        assertThat(count).isEqualTo(1.0);
    }

    @Test
    @DisplayName("should record migration timer with duration")
    void recordMigrationTimer() {
        metrics.recordMigration(1, 2, "success", Duration.ofSeconds(3));

        long timerCount = registry.timer(
            "customer_registry_schema_migration_duration_seconds",
            "from", "1", "to", "2"
        ).count();
        assertThat(timerCount).isEqualTo(1);
    }

    @Test
    @DisplayName("should record lock contention")
    void recordLockContention() {
        metrics.recordLockContention();
        metrics.recordLockContention();

        double count = registry.counter(
            "customer_registry_schema_migration_lock_contention_total"
        ).count();
        assertThat(count).isEqualTo(2.0);
    }

    @Test
    @DisplayName("should pre-register lock contention counter on construction")
    void preRegisterLockContentionCounter() {
        MeterRegistry freshRegistry = new SimpleMeterRegistry();
        new CustomerRegistryMetrics(freshRegistry);

        assertThat(freshRegistry.counter(
            "customer_registry_schema_migration_lock_contention_total"
        )).isNotNull();
    }
}
