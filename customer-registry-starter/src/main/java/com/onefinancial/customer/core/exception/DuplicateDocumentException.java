package com.onefinancial.customer.core.exception;

/**
 * Thrown when attempting to register a customer with a document
 * number that already exists in the registry.
 */
public class DuplicateDocumentException extends CustomerRegistryException {

    public DuplicateDocumentException() {
        super("A customer with this document already exists");
    }
}
