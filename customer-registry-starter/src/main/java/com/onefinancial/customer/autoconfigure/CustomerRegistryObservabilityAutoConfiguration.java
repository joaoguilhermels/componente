package com.onefinancial.customer.autoconfigure;

import com.onefinancial.customer.observability.CustomerRegistryMetrics;

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
 * <p>Activated when both {@code customer.registry.enabled=true} and
 * {@code customer.registry.features.observability=true} (dual-gate pattern).
 * This ensures the master switch can disable all features, including observability,
 * even when the individual feature flag is on.</p>
 *
 * <p>Additionally requires Micrometer on the classpath and a {@link MeterRegistry}
 * bean to register the metrics facade.</p>
 */
@AutoConfiguration(before = CustomerRegistryCoreAutoConfiguration.class)
@ConditionalOnProperty(
    prefix = "customer.registry",
    name = {"enabled", "features.observability"},
    havingValue = "true"
)
@ConditionalOnClass(MeterRegistry.class)
public class CustomerRegistryObservabilityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(MeterRegistry.class)
    public CustomerRegistryMetrics customerRegistryMetrics(MeterRegistry registry) {
        return new CustomerRegistryMetrics(registry);
    }
}
