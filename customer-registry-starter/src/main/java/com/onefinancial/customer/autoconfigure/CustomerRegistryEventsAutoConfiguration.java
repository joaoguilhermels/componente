package com.onefinancial.customer.autoconfigure;

import com.onefinancial.customer.events.CustomerEventsConfiguration;

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
 *
 * <p>ORDERING: Runs BEFORE CustomerRegistryCoreAutoConfiguration so the Spring event adapter
 *           registers before the NoOp fallback (which uses @ConditionalOnMissingBean).
 * GATE: customer.registry.enabled=true AND customer.registry.features.publish-events=true.
 * BRIDGE: Imports CustomerEventsConfiguration which exposes package-private SpringEventPublisherAdapter.
 * OVERRIDABLE: Host apps can provide their own CustomerEventPublisher bean.
 * See ADR-002 (bridge pattern) and ADR-003 (ordering rationale).</p>
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
