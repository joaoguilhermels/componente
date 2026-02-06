package com.oneff.customer.core.model;

import com.oneff.customer.core.exception.InvalidStatusTransitionException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate root for the Customer Registry bounded context.
 *
 * <p>Customers are created via the factory methods {@link #createPF} and {@link #createPJ},
 * which enforce type-document consistency. Status transitions are guarded — only valid
 * paths through the lifecycle state machine are allowed.</p>
 */
public final class Customer {

    private UUID id;
    private CustomerType type;
    private Document document;
    private String displayName;
    private CustomerStatus status;
    private List<Address> addresses;
    private List<Contact> contacts;
    private Attributes attributes;
    private Instant createdAt;
    private Instant updatedAt;

    private Customer() {
        // Used by builder/factory only
    }

    /**
     * Factory for creating a Pessoa Física (individual) customer.
     */
    public static Customer createPF(String cpfNumber, String displayName) {
        return create(CustomerType.PF, cpfNumber, displayName);
    }

    /**
     * Factory for creating a Pessoa Jurídica (legal entity) customer.
     */
    public static Customer createPJ(String cnpjNumber, String displayName) {
        return create(CustomerType.PJ, cnpjNumber, displayName);
    }

    private static Customer create(CustomerType type, String documentNumber, String displayName) {
        Objects.requireNonNull(displayName, "Display name must not be null");
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("Display name must not be blank");
        }

        var customer = new Customer();
        customer.id = UUID.randomUUID();
        customer.type = type;
        customer.document = new Document(type, documentNumber);
        customer.displayName = displayName.trim();
        customer.status = CustomerStatus.DRAFT;
        customer.addresses = new ArrayList<>();
        customer.contacts = new ArrayList<>();
        customer.attributes = Attributes.empty();
        customer.createdAt = Instant.now();
        customer.updatedAt = customer.createdAt;
        return customer;
    }

    /**
     * Reconstitutes a Customer from persistence (all fields provided).
     */
    public static Customer reconstitute(
            UUID id, CustomerType type, Document document, String displayName,
            CustomerStatus status, List<Address> addresses, List<Contact> contacts,
            Attributes attributes, Instant createdAt, Instant updatedAt) {
        var customer = new Customer();
        customer.id = id;
        customer.type = type;
        customer.document = document;
        customer.displayName = displayName;
        customer.status = status;
        customer.addresses = new ArrayList<>(addresses);
        customer.contacts = new ArrayList<>(contacts);
        customer.attributes = attributes;
        customer.createdAt = createdAt;
        customer.updatedAt = updatedAt;
        return customer;
    }

    public void transitionTo(CustomerStatus newStatus) {
        if (!status.canTransitionTo(newStatus)) {
            throw new InvalidStatusTransitionException(status, newStatus);
        }
        this.status = newStatus;
        this.updatedAt = Instant.now();
    }

    public void updateDisplayName(String newName) {
        Objects.requireNonNull(newName, "Display name must not be null");
        if (newName.isBlank()) {
            throw new IllegalArgumentException("Display name must not be blank");
        }
        this.displayName = newName.trim();
        this.updatedAt = Instant.now();
    }

    public void addAddress(Address address) {
        Objects.requireNonNull(address, "Address must not be null");
        this.addresses.add(address);
        this.updatedAt = Instant.now();
    }

    public void addContact(Contact contact) {
        Objects.requireNonNull(contact, "Contact must not be null");
        this.contacts.add(contact);
        this.updatedAt = Instant.now();
    }

    public void setAttributes(Attributes attributes) {
        this.attributes = Objects.requireNonNull(attributes, "Attributes must not be null");
        this.updatedAt = Instant.now();
    }

    // ─── Getters ─────────────────────────────────────────────────

    public UUID getId() { return id; }
    public CustomerType getType() { return type; }
    public Document getDocument() { return document; }
    public String getDisplayName() { return displayName; }
    public CustomerStatus getStatus() { return status; }
    public List<Address> getAddresses() { return Collections.unmodifiableList(addresses); }
    public List<Contact> getContacts() { return Collections.unmodifiableList(contacts); }
    public Attributes getAttributes() { return attributes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Customer other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Customer{id=%s, type=%s, document=%s, status=%s}".formatted(
            id, type, document.number(), status);
    }
}
