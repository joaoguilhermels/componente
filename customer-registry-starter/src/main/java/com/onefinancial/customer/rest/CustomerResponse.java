package com.onefinancial.customer.rest;

import com.onefinancial.customer.core.model.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Response DTO representing a customer.
 *
 * <p>This record is {@code public} intentionally so that consumers of the library
 * can reference it in their own tests, deserialization, and response handling code.</p>
 *
 * <p>The {@code document} field may contain either the full formatted document
 * (e.g. {@code 123.456.789-09}) or a masked version (e.g. {@code ***.***.*89-09})
 * depending on the endpoint. List endpoints mask documents for security; detail
 * endpoints return the full document.</p>
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

    /**
     * Creates a response with the full (unmasked) document.
     * Used by detail endpoints where the consumer needs the full document.
     */
    public static CustomerResponse from(Customer customer) {
        return build(customer, customer.getDocument().formatted());
    }

    /**
     * Creates a response with a masked document.
     * Used by list endpoints to avoid exposing full documents in bulk responses.
     *
     * <p>CPF masking: ***.***.*89-09 (last 4 digits visible).
     * CNPJ masking: **.***.**&#47;*001-95 (last 6 characters visible).</p>
     */
    public static CustomerResponse fromMasked(Customer customer) {
        return build(customer, maskDocument(customer.getDocument()));
    }

    private static CustomerResponse build(Customer customer, String documentValue) {
        return new CustomerResponse(
            customer.getId(),
            customer.getType().name(),
            documentValue,
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

    static String maskDocument(Document document) {
        String number = document.number();
        String formatted = document.formatted();
        // Mask all digits except the last 4
        int visibleCount = 4;
        int totalDigits = number.length();
        int maskedDigits = totalDigits - visibleCount;

        StringBuilder masked = new StringBuilder();
        int digitIndex = 0;
        for (int i = 0; i < formatted.length(); i++) {
            char ch = formatted.charAt(i);
            if (Character.isDigit(ch)) {
                masked.append(digitIndex < maskedDigits ? '*' : ch);
                digitIndex++;
            } else {
                masked.append(ch);
            }
        }
        return masked.toString();
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

    /**
     * Nested response DTO for a customer address.
     *
     * <p>This record is {@code public} intentionally for consumer reuse.</p>
     */
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

    /**
     * Nested response DTO for a customer contact.
     *
     * <p>This record is {@code public} intentionally for consumer reuse.</p>
     */
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
