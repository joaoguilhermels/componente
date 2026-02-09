package com.onefinancial.customer.core.model;

import java.util.UUID;

/**
 * Value object representing a contact channel for a customer (email, phone, etc.).
 */
public record Contact(
    UUID id,
    ContactType type,
    String value,
    boolean primary
) {
    public Contact {
        if (id == null) id = UUID.randomUUID();
    }

    public enum ContactType {
        EMAIL,
        PHONE,
        MOBILE,
        FAX
    }
}
