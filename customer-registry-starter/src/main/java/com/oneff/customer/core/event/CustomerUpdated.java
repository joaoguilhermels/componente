package com.oneff.customer.core.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when a customer's data is modified
 * (display name, addresses, contacts, or attributes).
 *
 * @param eventId    deterministic UUID for idempotent processing
 * @param customerId the updated customer's ID
 * @param occurredAt timestamp of the event
 */
public record CustomerUpdated(
    UUID eventId,
    UUID customerId,
    Instant occurredAt
) {
    public CustomerUpdated {
        java.util.Objects.requireNonNull(eventId, "eventId must not be null");
        java.util.Objects.requireNonNull(customerId, "customerId must not be null");
        java.util.Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    public static CustomerUpdated of(UUID customerId) {
        return new CustomerUpdated(
            UUID.nameUUIDFromBytes(("updated:" + customerId).getBytes()),
            customerId, Instant.now()
        );
    }
}
