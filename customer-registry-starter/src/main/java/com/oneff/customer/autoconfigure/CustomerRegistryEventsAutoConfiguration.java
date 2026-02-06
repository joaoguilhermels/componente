package com.oneff.customer.autoconfigure;

import com.oneff.customer.events.CustomerEventsConfiguration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for Customer Registry event publishing.
 *
 * <p>Gated by {@code customer.registry.enabled=true} and
 * {@code customer.registry.features.publish-events=true}.
 * Replaces the {@link NoOpEventPublisher} with the
 * Spring ApplicationEvent-based adapter.</p>
 */
@AutoConfiguration(before = CustomerRegistryCoreAutoConfiguration.class)
@ConditionalOnProperty(
    prefix = "customer.registry",
    name = {"enabled", "features.publish-events"},
    havingValue = "true"
)
@Import(CustomerEventsConfiguration.class)
public class CustomerRegistryEventsAutoConfiguration {
}
