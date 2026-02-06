package com.oneff.customer.persistence;

import com.oneff.customer.core.model.Contact;
import jakarta.persistence.*;

import java.util.UUID;

/**
 * JPA entity mapping for the {@code cr_contact} table.
 */
@Entity
@Table(name = "cr_contact")
class ContactEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer;

    @Enumerated(EnumType.STRING)
    @Column(name = "contact_type", nullable = false, length = 10)
    private Contact.ContactType type;

    @Column(name = "contact_value", nullable = false, length = 255)
    private String value;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    protected ContactEntity() {}

    // ─── Getters and Setters ─────────────────────────────────

    UUID getId() { return id; }
    void setId(UUID id) { this.id = id; }

    CustomerEntity getCustomer() { return customer; }
    void setCustomer(CustomerEntity customer) { this.customer = customer; }

    Contact.ContactType getType() { return type; }
    void setType(Contact.ContactType type) { this.type = type; }

    String getValue() { return value; }
    void setValue(String value) { this.value = value; }

    boolean isPrimary() { return primary; }
    void setPrimary(boolean primary) { this.primary = primary; }
}
