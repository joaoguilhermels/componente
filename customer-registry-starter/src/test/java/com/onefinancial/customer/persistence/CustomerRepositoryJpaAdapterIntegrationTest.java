package com.onefinancial.customer.persistence;

import com.onefinancial.customer.core.model.*;
import com.onefinancial.customer.core.port.CustomerRepository;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(CustomerRepositoryJpaAdapterIntegrationTest.TestConfig.class)
class CustomerRepositoryJpaAdapterIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("customer_registry_test")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("db/changelog/v1.0.0/001-create-customer-tables.sql");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.jpa.database-platform",
            () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.liquibase.enabled", () -> "false");
    }

    @Autowired
    private CustomerRepository repository;

    @Configuration
    @EnableAutoConfiguration
    @Import(CustomerPersistenceConfiguration.class)
    static class TestConfig {
    }

    // ─── Test Fixtures ────────────────────────────────────────

    private Customer createPfCustomer() {
        return Customer.createPF("52998224725", "Maria Silva");
    }

    private Customer createPjCustomer() {
        return Customer.createPJ("11222333000181", "Tech Corp LTDA");
    }

    // ─── Tests ────────────────────────────────────────────────

    @Test
    @DisplayName("should save and find PF customer by ID")
    void savesAndFindsPfById() {
        Customer customer = createPfCustomer();

        Customer saved = repository.save(customer);
        Optional<Customer> found = repository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(customer.getId());
        assertThat(found.get().getType()).isEqualTo(CustomerType.PF);
        assertThat(found.get().getDocument().number()).isEqualTo("52998224725");
        assertThat(found.get().getDisplayName()).isEqualTo("Maria Silva");
        assertThat(found.get().getStatus()).isEqualTo(CustomerStatus.DRAFT);
    }

    @Test
    @DisplayName("should save and find PJ customer by ID")
    void savesAndFindsPjById() {
        Customer customer = createPjCustomer();

        Customer saved = repository.save(customer);
        Optional<Customer> found = repository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getType()).isEqualTo(CustomerType.PJ);
        assertThat(found.get().getDocument().number()).isEqualTo("11222333000181");
        assertThat(found.get().getDisplayName()).isEqualTo("Tech Corp LTDA");
    }

    @Test
    @DisplayName("should find customer by document")
    void findsByDocument() {
        Customer customer = createPfCustomer();
        repository.save(customer);

        Document document = new Document(CustomerType.PF, "52998224725");
        Optional<Customer> found = repository.findByDocument(document);

        assertThat(found).isPresent();
        assertThat(found.get().getDocument().number()).isEqualTo("52998224725");
    }

    @Test
    @DisplayName("should return empty when document not found")
    void returnsEmptyForUnknownDocument() {
        Document document = new Document(CustomerType.PF, "52998224725");
        Optional<Customer> found = repository.findByDocument(document);

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("should check document existence")
    void checksDocumentExistence() {
        Customer customer = createPfCustomer();
        Document document = customer.getDocument();

        assertThat(repository.existsByDocument(document)).isFalse();

        repository.save(customer);

        assertThat(repository.existsByDocument(document)).isTrue();
    }

    @Test
    @DisplayName("should list all customers")
    void listsAllCustomers() {
        repository.save(createPfCustomer());
        repository.save(createPjCustomer());

        List<Customer> all = repository.findAll();

        assertThat(all).hasSize(2);
    }

    @Test
    @DisplayName("should persist and retrieve addresses")
    void persistsAddresses() {
        Customer customer = createPfCustomer();
        Address address = new Address(
            java.util.UUID.randomUUID(),
            "Rua das Flores", "123", "Apt 4B",
            "Centro", "São Paulo", "SP", "01001-000", "BR"
        );
        customer.addAddress(address);

        repository.save(customer);

        Customer found = repository.findById(customer.getId()).orElseThrow();
        assertThat(found.getAddresses()).hasSize(1);
        assertThat(found.getAddresses().getFirst().street()).isEqualTo("Rua das Flores");
        assertThat(found.getAddresses().getFirst().city()).isEqualTo("São Paulo");
    }

    @Test
    @DisplayName("should persist and retrieve contacts")
    void persistsContacts() {
        Customer customer = createPfCustomer();
        Contact contact = new Contact(
            java.util.UUID.randomUUID(),
            Contact.ContactType.EMAIL,
            "maria@example.com",
            true
        );
        customer.addContact(contact);

        repository.save(customer);

        Customer found = repository.findById(customer.getId()).orElseThrow();
        assertThat(found.getContacts()).hasSize(1);
        assertThat(found.getContacts().getFirst().type()).isEqualTo(Contact.ContactType.EMAIL);
        assertThat(found.getContacts().getFirst().value()).isEqualTo("maria@example.com");
        assertThat(found.getContacts().getFirst().primary()).isTrue();
    }

    @Test
    @DisplayName("should persist and retrieve JSONB attributes")
    void persistsJsonbAttributes() {
        Customer customer = createPfCustomer();
        Attributes attrs = Attributes.empty()
            .with("loyaltyTier", new AttributeValue.StringValue("GOLD"))
            .with("signupYear", new AttributeValue.IntegerValue(2024))
            .with("active", new AttributeValue.BooleanValue(true))
            .with("score", new AttributeValue.DecimalValue(new BigDecimal("98.5")))
            .with("birthDate", new AttributeValue.DateValue(LocalDate.of(1990, 6, 15)));
        customer.setAttributes(attrs);

        repository.save(customer);

        Customer found = repository.findById(customer.getId()).orElseThrow();
        Attributes foundAttrs = found.getAttributes();
        assertThat(foundAttrs.schemaVersion()).isEqualTo(1);
        assertThat(foundAttrs.get("loyaltyTier", AttributeValue.StringValue.class).value())
            .isEqualTo("GOLD");
        assertThat(foundAttrs.get("signupYear", AttributeValue.IntegerValue.class).value())
            .isEqualTo(2024);
        assertThat(foundAttrs.get("active", AttributeValue.BooleanValue.class).value())
            .isTrue();
        assertThat(foundAttrs.get("score", AttributeValue.DecimalValue.class).value())
            .isEqualByComparingTo(new BigDecimal("98.5"));
        assertThat(foundAttrs.get("birthDate", AttributeValue.DateValue.class).value())
            .isEqualTo(LocalDate.of(1990, 6, 15));
    }

    @Test
    @DisplayName("should update existing customer")
    void updatesExistingCustomer() {
        Customer customer = createPfCustomer();
        repository.save(customer);

        Customer found = repository.findById(customer.getId()).orElseThrow();
        found.updateDisplayName("Maria Silva Santos");
        repository.save(found);

        Customer updated = repository.findById(customer.getId()).orElseThrow();
        assertThat(updated.getDisplayName()).isEqualTo("Maria Silva Santos");
    }

    @Test
    @DisplayName("should return empty when ID not found")
    void returnsEmptyForUnknownId() {
        Optional<Customer> found = repository.findById(java.util.UUID.randomUUID());
        assertThat(found).isEmpty();
    }
}
