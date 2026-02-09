package com.onefinancial.customer.core.exception;

import java.util.Collections;
import java.util.List;

/**
 * Thrown when one or more customer validators reject a customer.
 * Contains all validation error message keys for batch error reporting.
 */
public class CustomerValidationException extends CustomerRegistryException {

    private final List<String> errorKeys;

    public CustomerValidationException(List<String> errorKeys) {
        super("Customer validation failed: %s".formatted(String.join(", ", errorKeys)));
        this.errorKeys = Collections.unmodifiableList(errorKeys);
    }

    public List<String> getErrorKeys() {
        return errorKeys;
    }
}
