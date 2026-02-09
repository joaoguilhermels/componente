package com.oneff.customer.events;

import com.oneff.customer.core.event.CustomerCreated;
import com.oneff.customer.core.event.CustomerDeleted;
import com.oneff.customer.core.event.CustomerStatusChanged;
import com.oneff.customer.core.event.CustomerUpdated;
import com.oneff.customer.core.port.CustomerEventPublisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Bridges domain events to Spring's {@link ApplicationEventPublisher}.
 *
 * <p>When Spring Modulith's event publication support is on the classpath,
 * events are automatically persisted via the outbox pattern, ensuring
 * at-least-once delivery with transactional guarantees.</p>
 */
class SpringEventPublisherAdapter implements CustomerEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SpringEventPublisherAdapter.class);

    private final ApplicationEventPublisher applicationEventPublisher;

    SpringEventPublisherAdapter(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(CustomerCreated event) {
        log.info("Publishing CustomerCreated event: customerId={}, eventId={}",
            event.customerId(), event.eventId());
        applicationEventPublisher.publishEvent(event);
    }

    @Override
    public void publish(CustomerUpdated event) {
        log.info("Publishing CustomerUpdated event: customerId={}, eventId={}",
            event.customerId(), event.eventId());
        applicationEventPublisher.publishEvent(event);
    }

    @Override
    public void publish(CustomerStatusChanged event) {
        log.info("Publishing CustomerStatusChanged event: customerId={}, {} -> {}, eventId={}",
            event.customerId(), event.fromStatus(), event.toStatus(), event.eventId());
        applicationEventPublisher.publishEvent(event);
    }

    @Override
    public void publish(CustomerDeleted event) {
        log.info("Publishing CustomerDeleted event: customerId={}, eventId={}",
            event.customerId(), event.eventId());
        applicationEventPublisher.publishEvent(event);
    }
}
