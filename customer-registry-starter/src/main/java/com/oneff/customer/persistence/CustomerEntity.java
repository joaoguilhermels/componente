package com.oneff.customer.persistence;

import com.oneff.customer.core.model.CustomerStatus;
import com.oneff.customer.core.model.CustomerType;
import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity mapping for the {@code cr_customer} table.
 *
 * <p>The {@code attributes} column is stored as PostgreSQL JSONB,
 * converted by {@link AttributesJsonSerializer}.</p>
 *
 * <p>Implements {@link Persistable} to avoid an extra SELECT on insert
 * when the ID is pre-assigned by the domain layer.</p>
 */
@Entity
@Table(name = "cr_customer")
class CustomerEntity implements Persistable<UUID> {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 2)
    private CustomerType type;

    @Column(name = "document_number", nullable = false, unique = true, length = 14)
    private String documentNumber;

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CustomerStatus status;

    @Column(name = "attributes", columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private String attributesJson;

    @Column(name = "schema_version", nullable = false)
    private int schemaVersion = 1;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @org.hibernate.annotations.BatchSize(size = 25)
    private List<AddressEntity> addresses = new ArrayList<>();

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @org.hibernate.annotations.BatchSize(size = 25)
    private List<ContactEntity> contacts = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    @Transient
    private boolean entityIsNew = false;

    protected CustomerEntity() {}

    // ─── Persistable ─────────────────────────────────────────

    @Override
    public UUID getId() { return id; }

    @Override
    public boolean isNew() { return entityIsNew; }

    void markAsNew() { this.entityIsNew = true; }

    // ─── JPA Audit Callbacks ─────────────────────────────────

    /**
     * Defensive fallback: sets {@code createdAt} and {@code updatedAt}
     * if not already set by the domain mapper.
     */
    @PrePersist
    void onPrePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    /**
     * Defensive fallback: updates {@code updatedAt} on every JPA update.
     */
    @PreUpdate
    void onPreUpdate() {
        updatedAt = Instant.now();
    }

    // ─── Getters and Setters ─────────────────────────────────

    void setId(UUID id) { this.id = id; }

    CustomerType getType() { return type; }
    void setType(CustomerType type) { this.type = type; }

    String getDocumentNumber() { return documentNumber; }
    void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }

    String getDisplayName() { return displayName; }
    void setDisplayName(String displayName) { this.displayName = displayName; }

    CustomerStatus getStatus() { return status; }
    void setStatus(CustomerStatus status) { this.status = status; }

    String getAttributesJson() { return attributesJson; }
    void setAttributesJson(String attributesJson) { this.attributesJson = attributesJson; }

    int getSchemaVersion() { return schemaVersion; }
    void setSchemaVersion(int schemaVersion) { this.schemaVersion = schemaVersion; }

    List<AddressEntity> getAddresses() { return addresses; }
    void setAddresses(List<AddressEntity> addresses) { this.addresses = addresses; }

    List<ContactEntity> getContacts() { return contacts; }
    void setContacts(List<ContactEntity> contacts) { this.contacts = contacts; }

    Instant getCreatedAt() { return createdAt; }
    void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    Instant getUpdatedAt() { return updatedAt; }
    void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    Long getVersion() { return version; }
}
