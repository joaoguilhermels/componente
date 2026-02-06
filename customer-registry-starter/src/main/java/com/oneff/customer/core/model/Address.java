package com.oneff.customer.core.model;

import java.util.UUID;

/**
 * Value object representing a postal address associated with a customer.
 */
public record Address(
    UUID id,
    String street,
    String number,
    String complement,
    String neighborhood,
    String city,
    String state,
    String zipCode,
    String country
) {
    public Address {
        if (id == null) id = UUID.randomUUID();
        if (country == null || country.isBlank()) country = "BR";
    }
}
