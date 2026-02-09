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
    public static CustomerDeleted of(UUID customerId) {
        return new CustomerDeleted(
            UUID.nameUUIDFromBytes(("deleted:" + customerId).getBytes()),
            customerId, Instant.now()
        );
    }
}
