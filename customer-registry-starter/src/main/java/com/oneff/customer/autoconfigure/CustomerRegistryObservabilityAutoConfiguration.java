package com.oneff.customer.autoconfigure;

import com.oneff.customer.observability.CustomerRegistryMetrics;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for Customer Registry observability.
 *
 * <p>Activated when {@code customer.registry.enabled=true} and
 * Micrometer is on the classpath. Registers the metrics facade.</p>
 */
@AutoConfiguration(after = CustomerRegistryCoreAutoConfiguration.class)
@ConditionalOnProperty(name = "customer.registry.enabled", havingValue = "true")
@ConditionalOnClass(MeterRegistry.class)
public class CustomerRegistryObservabilityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(MeterRegistry.class)
    public CustomerRegistryMetrics customerRegistryMetrics(MeterRegistry registry) {
        return new CustomerRegistryMetrics(registry);
    }
}
