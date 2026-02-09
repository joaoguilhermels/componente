package com.oneff.customer.core.port;

import com.oneff.customer.core.event.CustomerCreated;
import com.oneff.customer.core.event.CustomerDeleted;
import com.oneff.customer.core.event.CustomerStatusChanged;
import com.oneff.customer.core.event.CustomerUpdated;

/**
 * Port for publishing domain events related to customer lifecycle changes.
 *
 * <p>Implementations may use Spring ApplicationEvents, a message broker,
 * or a no-op fallback when event publishing is disabled.</p>
 */
public interface CustomerEventPublisher {

    void publish(CustomerCreated event);

    void publish(CustomerUpdated event);

    void publish(CustomerStatusChanged event);

    void publish(CustomerDeleted event);
}
