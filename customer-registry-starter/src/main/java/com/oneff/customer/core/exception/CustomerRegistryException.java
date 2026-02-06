package com.oneff.customer.core.exception;

/**
 * Base exception for all Customer Registry domain errors.
 */
public class CustomerRegistryException extends RuntimeException {

    public CustomerRegistryException(String message) {
        super(message);
    }

    public CustomerRegistryException(String message, Throwable cause) {
        super(message, cause);
    }
}
