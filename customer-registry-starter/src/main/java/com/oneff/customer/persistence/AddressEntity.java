package com.oneff.customer.persistence;

import jakarta.persistence.*;

import java.util.UUID;

/**
 * JPA entity mapping for the {@code cr_address} table.
 */
@Entity
@Table(name = "cr_address")
class AddressEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer;

    @Column(length = 255)
    private String street;

    @Column(name = "address_number", length = 20)
    private String number;

    @Column(length = 100)
    private String complement;

    @Column(length = 100)
    private String neighborhood;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 2)
    private String state;

    @Column(name = "zip_code", nullable = false, length = 10)
    private String zipCode;

    @Column(nullable = false, length = 2)
    private String country;

    protected AddressEntity() {}

    // ─── Getters and Setters ─────────────────────────────────

    UUID getId() { return id; }
    void setId(UUID id) { this.id = id; }

    CustomerEntity getCustomer() { return customer; }
    void setCustomer(CustomerEntity customer) { this.customer = customer; }

    String getStreet() { return street; }
    void setStreet(String street) { this.street = street; }

    String getNumber() { return number; }
    void setNumber(String number) { this.number = number; }

    String getComplement() { return complement; }
    void setComplement(String complement) { this.complement = complement; }

    String getNeighborhood() { return neighborhood; }
    void setNeighborhood(String neighborhood) { this.neighborhood = neighborhood; }

    String getCity() { return city; }
    void setCity(String city) { this.city = city; }

    String getState() { return state; }
    void setState(String state) { this.state = state; }

    String getZipCode() { return zipCode; }
    void setZipCode(String zipCode) { this.zipCode = zipCode; }

    String getCountry() { return country; }
    void setCountry(String country) { this.country = country; }
}
