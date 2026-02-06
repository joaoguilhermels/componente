package com.oneff.customer.core.exception;

/**
 * Thrown when a CPF or CNPJ document number fails validation
 * (wrong digit count, invalid checksum, or all-same-digits).
 */
public class DocumentValidationException extends CustomerRegistryException {

    public DocumentValidationException(String message) {
        super(message);
    }
}
