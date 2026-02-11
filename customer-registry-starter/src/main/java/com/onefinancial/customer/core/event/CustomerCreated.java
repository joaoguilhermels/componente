package com.onefinancial.customer.core.event;

import com.onefinancial.customer.core.model.CustomerType;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when a new customer is registered.
 *
 * @param eventId      unique UUID for idempotent processing
 * @param customerId   the newly created customer's ID
 * @param customerType PF or PJ
 * @param occurredAt   timestamp of the event
 */
public record CustomerCreated(
    UUID eventId,
    UUID customerId,
    CustomerType customerType,
    Instant occurredAt
) {
    public CustomerCreated {
        java.util.Objects.requireNonNull(eventId, "eventId must not be null");
        java.util.Objects.requireNonNull(customerId, "customerId must not be null");
        java.util.Objects.requireNonNull(customerType, "customerType must not be null");
        java.util.Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    public static CustomerCreated of(UUID customerId, CustomerType type) {
        Instant occurredAt = Instant.now();
        return new CustomerCreated(
            UUID.nameUUIDFromBytes(
                ("created:" + customerId + ":" + occurredAt).getBytes(java.nio.charset.StandardCharsets.UTF_8)),
            customerId, type, occurredAt
        );
    }
}
