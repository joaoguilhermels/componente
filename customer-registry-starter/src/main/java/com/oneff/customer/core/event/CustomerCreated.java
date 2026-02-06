package com.oneff.customer.core.event;

import com.oneff.customer.core.model.CustomerType;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when a new customer is registered.
 *
 * @param eventId      deterministic UUID for idempotent processing
 * @param customerId   the newly created customer's ID
 * @param customerType PF or PJ
 * @param document     raw document number
 * @param occurredAt   timestamp of the event
 */
public record CustomerCreated(
    UUID eventId,
    UUID customerId,
    CustomerType customerType,
    String document,
    Instant occurredAt
) {
    public static CustomerCreated of(UUID customerId, CustomerType type, String document) {
        return new CustomerCreated(
            UUID.nameUUIDFromBytes(("created:" + customerId).getBytes()),
            customerId, type, document, Instant.now()
        );
    }
}
