package com.oneff.customer.autoconfigure;

import com.oneff.customer.observability.CustomerRegistryMetrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerRegistryObservabilityAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(
            CustomerRegistryCoreAutoConfiguration.class,
            CustomerRegistryObservabilityAutoConfiguration.class
        ));

    @Test
    @DisplayName("should register metrics bean when Micrometer present and registry enabled")
    void registerMetricsWhenEnabled() {
        contextRunner
            .withPropertyValues("customer.registry.enabled=true")
            .withUserConfiguration(MicrometerConfig.class)
            .run(context ->
                assertThat(context).hasSingleBean(CustomerRegistryMetrics.class));
    }

    @Test
    @DisplayName("should not register metrics when registry disabled")
    void noMetricsWhenDisabled() {
        contextRunner
            .withUserConfiguration(MicrometerConfig.class)
            .run(context ->
                assertThat(context).doesNotHaveBean(CustomerRegistryMetrics.class));
    }

    @Test
    @DisplayName("should not register metrics when no MeterRegistry bean")
    void noMetricsWithoutMeterRegistry() {
        contextRunner
            .withPropertyValues("customer.registry.enabled=true")
            .run(context ->
                assertThat(context).doesNotHaveBean(CustomerRegistryMetrics.class));
    }

    @Configuration
    static class MicrometerConfig {
        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }
}
