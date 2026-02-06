package com.oneff.customer.core.exception;

import com.oneff.customer.core.model.CustomerStatus;

/**
 * Thrown when attempting an invalid customer status transition
 * (e.g., CLOSED → ACTIVE).
 */
public class InvalidStatusTransitionException extends CustomerRegistryException {

    public InvalidStatusTransitionException(CustomerStatus from, CustomerStatus to) {
        super("Cannot transition from %s to %s".formatted(from, to));
    }
}
