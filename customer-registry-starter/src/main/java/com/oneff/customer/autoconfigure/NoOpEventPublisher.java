package com.oneff.customer.autoconfigure;

import com.oneff.customer.core.event.CustomerCreated;
import com.oneff.customer.core.event.CustomerDeleted;
import com.oneff.customer.core.event.CustomerStatusChanged;
import com.oneff.customer.core.event.CustomerUpdated;
import com.oneff.customer.core.port.CustomerEventPublisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * No-op implementation of {@link CustomerEventPublisher}.
 *
 * <p>Used as a fallback when event publishing is disabled.
 * Logs events at DEBUG level for development visibility.</p>
 */
class NoOpEventPublisher implements CustomerEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(NoOpEventPublisher.class);

    @Override
    public void publish(CustomerCreated event) {
        log.debug("Event publishing disabled — would publish: {}", event);
    }

    @Override
    public void publish(CustomerUpdated event) {
        log.debug("Event publishing disabled — would publish: {}", event);
    }

    @Override
    public void publish(CustomerStatusChanged event) {
        log.debug("Event publishing disabled — would publish: {}", event);
    }

    @Override
    public void publish(CustomerDeleted event) {
        log.debug("Event publishing disabled — would publish: {}", event);
    }
}
