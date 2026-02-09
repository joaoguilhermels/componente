package com.oneff.customer.core.exception;

import java.util.UUID;

/**
 * Thrown when a customer cannot be found by the given identifier.
 */
public class CustomerNotFoundException extends CustomerRegistryException {

    public CustomerNotFoundException(UUID customerId) {
        super("Customer not found: " + customerId);
    }
}
