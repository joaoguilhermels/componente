package com.oneff.customer.core.event;

import com.oneff.customer.core.model.CustomerStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when a customer's lifecycle status changes.
 *
 * @param eventId    deterministic UUID for idempotent processing
 * @param customerId the customer whose status changed
 * @param fromStatus the previous status
 * @param toStatus   the new status
 * @param occurredAt timestamp of the event
 */
public record CustomerStatusChanged(
    UUID eventId,
    UUID customerId,
    CustomerStatus fromStatus,
    CustomerStatus toStatus,
    Instant occurredAt
) {
    public static CustomerStatusChanged of(UUID customerId, CustomerStatus from, CustomerStatus to) {
        return new CustomerStatusChanged(
            UUID.nameUUIDFromBytes(("status:" + customerId + ":" + from + ":" + to).getBytes()),
            customerId, from, to, Instant.now()
        );
    }
}
