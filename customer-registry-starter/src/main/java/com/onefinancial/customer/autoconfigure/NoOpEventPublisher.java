package com.onefinancial.customer.autoconfigure;

import com.onefinancial.customer.core.event.CustomerCreated;
import com.onefinancial.customer.core.event.CustomerDeleted;
import com.onefinancial.customer.core.event.CustomerStatusChanged;
import com.onefinancial.customer.core.event.CustomerUpdated;
import com.onefinancial.customer.core.port.CustomerEventPublisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * No-op implementation of {@link CustomerEventPublisher}.
 *
 * <p>Used as a fallback when event publishing is disabled.
 * Logs a WARN at startup; individual discards are logged at DEBUG.</p>
 */
// Fallback publisher when publish-events feature is disabled.
// Registered by CoreAutoConfiguration via @ConditionalOnMissingBean.
// Replaced by SpringEventPublisherAdapter when customer.registry.features.publish-events=true.
class NoOpEventPublisher implements CustomerEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(NoOpEventPublisher.class);

    NoOpEventPublisher() {
        log.warn("Customer Registry event publishing is DISABLED. "
            + "Events will NOT be published. To enable, add spring-events adapter to classpath "
            + "and set customer.registry.features.publish-events=true");
    }

    @Override
    public void publish(CustomerCreated event) {
        log.debug("Event publishing disabled — discarding CustomerCreated: customerId={}", event.customerId());
    }

    @Override
    public void publish(CustomerUpdated event) {
        log.debug("Event publishing disabled — discarding CustomerUpdated: customerId={}", event.customerId());
    }

    @Override
    public void publish(CustomerStatusChanged event) {
        log.debug("Event publishing disabled — discarding CustomerStatusChanged: customerId={}", event.customerId());
    }

    @Override
    public void publish(CustomerDeleted event) {
        log.debug("Event publishing disabled — discarding CustomerDeleted: customerId={}", event.customerId());
    }
}
