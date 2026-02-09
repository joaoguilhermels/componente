package com.oneff.customer.core.exception;

import java.util.UUID;

/**
 * Thrown when a customer cannot be found by the given identifier.
 */
public class CustomerNotFoundException extends CustomerRegistryException {

    private final UUID customerId;

    public CustomerNotFoundException(UUID customerId) {
        super("Customer not found: " + java.util.Objects.requireNonNull(customerId, "customerId must not be null"));
        this.customerId = customerId;
    }

    public UUID getCustomerId() {
        return customerId;
    }
}
