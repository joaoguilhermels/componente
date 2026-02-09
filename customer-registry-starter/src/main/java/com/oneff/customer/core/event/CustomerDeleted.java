package com.oneff.customer.core.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when a customer is deleted.
 *
 * @param eventId    deterministic UUID for idempotent processing
 * @param customerId the deleted customer's ID
 * @param occurredAt timestamp of the event
 */
public record CustomerDeleted(
    UUID eventId,
    UUID customerId,
    Instant occurredAt
) {
    public CustomerDeleted {
        java.util.Objects.requireNonNull(eventId, "eventId must not be null");
        java.util.Objects.requireNonNull(customerId, "customerId must not be null");
        java.util.Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    public static CustomerDeleted of(UUID customerId) {
        return new CustomerDeleted(
            UUID.nameUUIDFromBytes(("deleted:" + customerId).getBytes()),
            customerId, Instant.now()
        );
    }
}
