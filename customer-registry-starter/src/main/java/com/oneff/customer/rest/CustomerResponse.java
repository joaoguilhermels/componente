package com.oneff.customer.rest;

import com.oneff.customer.core.model.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Response DTO representing a customer.
 */
public record CustomerResponse(
    UUID id,
    String type,
    String document,
    String displayName,
    String status,
    List<AddressResponse> addresses,
    List<ContactResponse> contacts,
    int schemaVersion,
    Map<String, Object> attributes,
    Instant createdAt,
    Instant updatedAt
) {

    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
            customer.getId(),
            customer.getType().name(),
            customer.getDocument().formatted(),
            customer.getDisplayName(),
            customer.getStatus().name(),
            customer.getAddresses().stream()
                .map(AddressResponse::from)
                .toList(),
            customer.getContacts().stream()
                .map(ContactResponse::from)
                .toList(),
            customer.getAttributes().schemaVersion(),
            customer.getAttributes().values().entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    e -> unwrapValue(e.getValue())
                )),
            customer.getCreatedAt(),
            customer.getUpdatedAt()
        );
    }

    private static Object unwrapValue(AttributeValue value) {
        return switch (value) {
            case AttributeValue.StringValue v -> v.value();
            case AttributeValue.IntegerValue v -> v.value();
            case AttributeValue.BooleanValue v -> v.value();
            case AttributeValue.DecimalValue v -> v.value();
            case AttributeValue.DateValue v -> v.value().toString();
        };
    }

    public record AddressResponse(
        UUID id, String street, String number, String complement,
        String neighborhood, String city, String state,
        String zipCode, String country
    ) {
        static AddressResponse from(Address a) {
            return new AddressResponse(
                a.id(), a.street(), a.number(), a.complement(),
                a.neighborhood(), a.city(), a.state(),
                a.zipCode(), a.country()
            );
        }
    }

    public record ContactResponse(
        UUID id, String type, String value, boolean primary
    ) {
        static ContactResponse from(Contact c) {
            return new ContactResponse(
                c.id(), c.type().name(), c.value(), c.primary()
            );
        }
    }
}
