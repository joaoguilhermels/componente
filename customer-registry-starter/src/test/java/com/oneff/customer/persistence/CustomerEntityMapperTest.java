package com.oneff.customer.persistence;

import com.oneff.customer.core.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerEntityMapperTest {

    private static final String VALID_CPF = "52998224725";
    private static final String VALID_CNPJ = "11222333000181";

    @Nested
    @DisplayName("toEntity")
    class ToEntity {

        @Test
        @DisplayName("should map all Customer fields to CustomerEntity")
        void mapsAllFields() {
            Customer customer = createFullCustomer();

            CustomerEntity entity = CustomerEntityMapper.toEntity(customer, true);

            assertThat(entity.getId()).isEqualTo(customer.getId());
            assertThat(entity.getType()).isEqualTo(CustomerType.PF);
            assertThat(entity.getDocumentNumber()).isEqualTo(VALID_CPF);
            assertThat(entity.getDisplayName()).isEqualTo("Maria Silva");
            assertThat(entity.getStatus()).isEqualTo(CustomerStatus.DRAFT);
            assertThat(entity.getSchemaVersion()).isEqualTo(customer.getAttributes().schemaVersion());
            assertThat(entity.getCreatedAt()).isEqualTo(customer.getCreatedAt());
            assertThat(entity.getUpdatedAt()).isEqualTo(customer.getUpdatedAt());
        }

        @Test
        @DisplayName("should map addresses to entity list")
        void mapsAddresses() {
            Customer customer = createFullCustomer();

            CustomerEntity entity = CustomerEntityMapper.toEntity(customer, true);

            assertThat(entity.getAddresses()).hasSize(1);
            AddressEntity addr = entity.getAddresses().get(0);
            assertThat(addr.getStreet()).isEqualTo("Rua das Flores");
            assertThat(addr.getNumber()).isEqualTo("123");
            assertThat(addr.getComplement()).isEqualTo("Apto 4");
            assertThat(addr.getNeighborhood()).isEqualTo("Centro");
            assertThat(addr.getCity()).isEqualTo("São Paulo");
            assertThat(addr.getState()).isEqualTo("SP");
            assertThat(addr.getZipCode()).isEqualTo("01000-000");
            assertThat(addr.getCountry()).isEqualTo("BR");
            assertThat(addr.getCustomer()).isSameAs(entity);
        }

        @Test
        @DisplayName("should map contacts to entity list")
        void mapsContacts() {
            Customer customer = createFullCustomer();

            CustomerEntity entity = CustomerEntityMapper.toEntity(customer, true);

            assertThat(entity.getContacts()).hasSize(1);
            ContactEntity contact = entity.getContacts().get(0);
            assertThat(contact.getType()).isEqualTo(Contact.ContactType.EMAIL);
            assertThat(contact.getValue()).isEqualTo("maria@example.com");
            assertThat(contact.isPrimary()).isTrue();
            assertThat(contact.getCustomer()).isSameAs(entity);
        }

        @Test
        @DisplayName("should map empty addresses and contacts")
        void mapsEmptyCollections() {
            Customer customer = Customer.createPF(VALID_CPF, "Test");

            CustomerEntity entity = CustomerEntityMapper.toEntity(customer, true);

            assertThat(entity.getAddresses()).isEmpty();
            assertThat(entity.getContacts()).isEmpty();
        }

        @Test
        @DisplayName("should serialize attributes to JSON string")
        void serializesAttributes() {
            Customer customer = Customer.createPF(VALID_CPF, "Test");
            customer.setAttributes(Attributes.empty()
                .with("tier", new AttributeValue.StringValue("GOLD")));

            CustomerEntity entity = CustomerEntityMapper.toEntity(customer, true);

            assertThat(entity.getAttributesJson()).contains("GOLD");
            assertThat(entity.getAttributesJson()).contains("STRING");
        }
    }

    @Nested
    @DisplayName("toDomain")
    class ToDomain {

        @Test
        @DisplayName("should map all CustomerEntity fields back to domain")
        void mapsAllFieldsBack() {
            Customer original = createFullCustomer();
            CustomerEntity entity = CustomerEntityMapper.toEntity(original, true);

            Customer reconstituted = CustomerEntityMapper.toDomain(entity);

            assertThat(reconstituted.getId()).isEqualTo(original.getId());
            assertThat(reconstituted.getType()).isEqualTo(CustomerType.PF);
            assertThat(reconstituted.getDocument().number()).isEqualTo(VALID_CPF);
            assertThat(reconstituted.getDisplayName()).isEqualTo("Maria Silva");
            assertThat(reconstituted.getStatus()).isEqualTo(CustomerStatus.DRAFT);
            assertThat(reconstituted.getCreatedAt()).isEqualTo(original.getCreatedAt());
            assertThat(reconstituted.getUpdatedAt()).isEqualTo(original.getUpdatedAt());
        }

        @Test
        @DisplayName("should map addresses back to domain objects")
        void mapsAddressesBack() {
            Customer original = createFullCustomer();
            CustomerEntity entity = CustomerEntityMapper.toEntity(original, true);

            Customer reconstituted = CustomerEntityMapper.toDomain(entity);

            assertThat(reconstituted.getAddresses()).hasSize(1);
            Address addr = reconstituted.getAddresses().get(0);
            assertThat(addr.street()).isEqualTo("Rua das Flores");
            assertThat(addr.number()).isEqualTo("123");
            assertThat(addr.complement()).isEqualTo("Apto 4");
            assertThat(addr.city()).isEqualTo("São Paulo");
        }

        @Test
        @DisplayName("should map contacts back to domain objects")
        void mapsContactsBack() {
            Customer original = createFullCustomer();
            CustomerEntity entity = CustomerEntityMapper.toEntity(original, true);

            Customer reconstituted = CustomerEntityMapper.toDomain(entity);

            assertThat(reconstituted.getContacts()).hasSize(1);
            Contact contact = reconstituted.getContacts().get(0);
            assertThat(contact.type()).isEqualTo(Contact.ContactType.EMAIL);
            assertThat(contact.value()).isEqualTo("maria@example.com");
            assertThat(contact.primary()).isTrue();
        }

        @Test
        @DisplayName("should map empty collections correctly")
        void mapsEmptyCollections() {
            Customer original = Customer.createPF(VALID_CPF, "Test");
            CustomerEntity entity = CustomerEntityMapper.toEntity(original, true);

            Customer reconstituted = CustomerEntityMapper.toDomain(entity);

            assertThat(reconstituted.getAddresses()).isEmpty();
            assertThat(reconstituted.getContacts()).isEmpty();
        }

        @Test
        @DisplayName("should deserialize attributes back correctly")
        void deserializesAttributes() {
            Customer original = Customer.createPF(VALID_CPF, "Test");
            original.setAttributes(Attributes.empty()
                .with("tier", new AttributeValue.StringValue("GOLD"))
                .with("score", new AttributeValue.DecimalValue(new BigDecimal("99.5")))
                .with("active", new AttributeValue.BooleanValue(true))
                .with("year", new AttributeValue.IntegerValue(2024))
                .with("joined", new AttributeValue.DateValue(LocalDate.of(2024, 1, 15))));
            CustomerEntity entity = CustomerEntityMapper.toEntity(original, true);

            Customer reconstituted = CustomerEntityMapper.toDomain(entity);

            assertThat(reconstituted.getAttributes().size()).isEqualTo(5);
            assertThat(reconstituted.getAttributes()
                .get("tier", AttributeValue.StringValue.class).value()).isEqualTo("GOLD");
            assertThat(reconstituted.getAttributes()
                .get("score", AttributeValue.DecimalValue.class).value())
                .isEqualByComparingTo(new BigDecimal("99.5"));
        }

        @Test
        @DisplayName("should handle null complement in address")
        void handlesNullComplement() {
            Customer customer = Customer.createPF(VALID_CPF, "Test");
            customer.addAddress(new Address(null, "Rua A", "1", null, "Bairro",
                "Cidade", "UF", "12345-678", "BR"));
            CustomerEntity entity = CustomerEntityMapper.toEntity(customer, true);

            Customer reconstituted = CustomerEntityMapper.toDomain(entity);

            assertThat(reconstituted.getAddresses().get(0).complement()).isNull();
        }
    }

    @Nested
    @DisplayName("Full round-trip")
    class FullRoundTrip {

        @Test
        @DisplayName("should maintain data integrity through toEntity→toDomain round-trip")
        void fullRoundTrip() {
            Customer original = createFullCustomer();

            CustomerEntity entity = CustomerEntityMapper.toEntity(original, true);
            Customer reconstituted = CustomerEntityMapper.toDomain(entity);

            assertThat(reconstituted.getId()).isEqualTo(original.getId());
            assertThat(reconstituted.getType()).isEqualTo(original.getType());
            assertThat(reconstituted.getDocument()).isEqualTo(original.getDocument());
            assertThat(reconstituted.getDisplayName()).isEqualTo(original.getDisplayName());
            assertThat(reconstituted.getStatus()).isEqualTo(original.getStatus());
            assertThat(reconstituted.getAddresses()).hasSize(original.getAddresses().size());
            assertThat(reconstituted.getContacts()).hasSize(original.getContacts().size());
            assertThat(reconstituted.getAttributes().size())
                .isEqualTo(original.getAttributes().size());
        }
    }

    // ─── Helpers ──────────────────────────────────────────────

    private Customer createFullCustomer() {
        Customer customer = Customer.createPF(VALID_CPF, "Maria Silva");
        customer.addAddress(new Address(null, "Rua das Flores", "123", "Apto 4",
            "Centro", "São Paulo", "SP", "01000-000", "BR"));
        customer.addContact(new Contact(null, Contact.ContactType.EMAIL,
            "maria@example.com", true));
        customer.setAttributes(Attributes.empty()
            .with("tier", new AttributeValue.StringValue("GOLD")));
        return customer;
    }
}
