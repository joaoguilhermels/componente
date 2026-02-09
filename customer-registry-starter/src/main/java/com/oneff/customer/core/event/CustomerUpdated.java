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
    public static CustomerUpdated of(UUID customerId) {
        return new CustomerUpdated(
            UUID.nameUUIDFromBytes(("updated:" + customerId).getBytes()),
            customerId, Instant.now()
        );
    }
}
