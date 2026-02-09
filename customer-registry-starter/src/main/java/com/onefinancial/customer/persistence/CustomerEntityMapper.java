package com.onefinancial.customer.persistence;

import com.onefinancial.customer.core.model.*;

import java.util.List;

/**
 * Maps between domain objects ({@link Customer}) and JPA entities ({@link CustomerEntity}).
 */
final class CustomerEntityMapper {

    private CustomerEntityMapper() {}

    static CustomerEntity toEntity(Customer customer, boolean isNew) {
        var entity = new CustomerEntity();
        entity.setId(customer.getId());
        entity.setType(customer.getType());
        entity.setDocumentNumber(customer.getDocument().number());
        entity.setDisplayName(customer.getDisplayName());
        entity.setStatus(customer.getStatus());
        entity.setAttributesJson(AttributesJsonSerializer.toJson(customer.getAttributes()));
        entity.setSchemaVersion(customer.getAttributes().schemaVersion());
        entity.setCreatedAt(customer.getCreatedAt());
        entity.setUpdatedAt(customer.getUpdatedAt());
        if (isNew) {
            entity.markAsNew();
        }

        List<AddressEntity> addressEntities = customer.getAddresses().stream()
            .map(addr -> toAddressEntity(addr, entity))
            .toList();
        entity.setAddresses(new java.util.ArrayList<>(addressEntities));

        List<ContactEntity> contactEntities = customer.getContacts().stream()
            .map(contact -> toContactEntity(contact, entity))
            .toList();
        entity.setContacts(new java.util.ArrayList<>(contactEntities));

        return entity;
    }

    static Customer toDomain(CustomerEntity entity) {
        Document document = new Document(entity.getType(), entity.getDocumentNumber());
        Attributes attributes = AttributesJsonSerializer.fromJson(entity.getAttributesJson());

        List<Address> addresses = entity.getAddresses().stream()
            .map(CustomerEntityMapper::toAddressDomain)
            .toList();

        List<Contact> contacts = entity.getContacts().stream()
            .map(CustomerEntityMapper::toContactDomain)
            .toList();

        return Customer.reconstitute(
            entity.getId(),
            entity.getType(),
            document,
            entity.getDisplayName(),
            entity.getStatus(),
            addresses,
            contacts,
            attributes,
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    private static AddressEntity toAddressEntity(Address address, CustomerEntity customer) {
        var entity = new AddressEntity();
        entity.setId(address.id());
        entity.setCustomer(customer);
        entity.setStreet(address.street());
        entity.setNumber(address.number());
        entity.setComplement(address.complement());
        entity.setNeighborhood(address.neighborhood());
        entity.setCity(address.city());
        entity.setState(address.state());
        entity.setZipCode(address.zipCode());
        entity.setCountry(address.country());
        return entity;
    }

    private static Address toAddressDomain(AddressEntity entity) {
        return new Address(
            entity.getId(),
            entity.getStreet(),
            entity.getNumber(),
            entity.getComplement(),
            entity.getNeighborhood(),
            entity.getCity(),
            entity.getState(),
            entity.getZipCode(),
            entity.getCountry()
        );
    }

    private static ContactEntity toContactEntity(Contact contact, CustomerEntity customer) {
        var entity = new ContactEntity();
        entity.setId(contact.id());
        entity.setCustomer(customer);
        entity.setType(contact.type());
        entity.setValue(contact.value());
        entity.setPrimary(contact.primary());
        return entity;
    }

    private static Contact toContactDomain(ContactEntity entity) {
        return new Contact(
            entity.getId(),
            entity.getType(),
            entity.getValue(),
            entity.isPrimary()
        );
    }
}
