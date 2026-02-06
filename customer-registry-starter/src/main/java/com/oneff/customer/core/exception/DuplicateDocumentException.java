package com.oneff.customer.core.exception;

/**
 * Thrown when attempting to register a customer with a document
 * number that already exists in the registry.
 */
public class DuplicateDocumentException extends CustomerRegistryException {

    public DuplicateDocumentException(String document) {
        super("Customer with document '%s' already exists".formatted(document));
    }
}
