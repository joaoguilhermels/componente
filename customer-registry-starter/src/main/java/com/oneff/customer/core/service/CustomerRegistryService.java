package com.oneff.customer.core.service;

import com.oneff.customer.core.event.CustomerCreated;
import com.oneff.customer.core.event.CustomerStatusChanged;
import com.oneff.customer.core.event.CustomerUpdated;
import com.oneff.customer.core.exception.CustomerValidationException;
import com.oneff.customer.core.exception.DuplicateDocumentException;
import com.oneff.customer.core.model.Customer;
import com.oneff.customer.core.model.CustomerStatus;
import com.oneff.customer.core.model.Document;
import com.oneff.customer.core.port.CustomerEventPublisher;
import com.oneff.customer.core.port.CustomerRepository;
import com.oneff.customer.core.spi.CustomerEnricher;
import com.oneff.customer.core.spi.CustomerValidator;
import com.oneff.customer.core.spi.ValidationResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Core domain service orchestrating the customer registration pipeline.
 *
 * <p>Pipeline: validate all → check duplicate → enrich all → persist → publish event.
 *
 * <p>This service has no Spring annotations by design — it's wired by
 * the auto-configuration layer, allowing it to be tested in isolation.</p>
 */
public class CustomerRegistryService {

    private final List<CustomerValidator> validators;
    private final List<CustomerEnricher> enrichers;
    private final CustomerRepository repository;
    private final CustomerEventPublisher eventPublisher;

    public CustomerRegistryService(
            List<CustomerValidator> validators,
            List<CustomerEnricher> enrichers,
            CustomerRepository repository,
            CustomerEventPublisher eventPublisher) {
        this.validators = validators != null ? validators : List.of();
        this.enrichers = enrichers != null ? enrichers : List.of();
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Registers a new customer through the full pipeline.
     *
     * @param customer the customer to register (must be in DRAFT status)
     * @return the persisted customer
     * @throws CustomerValidationException if any validator rejects the customer
     * @throws DuplicateDocumentException if the document already exists
     */
    public Customer register(Customer customer) {
        runValidators(customer);
        checkDuplicate(customer.getDocument());
        customer = runEnrichers(customer);
        Customer saved = repository.save(customer);
        eventPublisher.publish(CustomerCreated.of(
            saved.getId(), saved.getType(), saved.getDocument().number()));
        return saved;
    }

    /**
     * Updates an existing customer's data.
     */
    public Customer update(Customer customer) {
        runValidators(customer);
        Customer saved = repository.save(customer);
        eventPublisher.publish(CustomerUpdated.of(saved.getId()));
        return saved;
    }

    /**
     * Transitions a customer to a new lifecycle status.
     */
    public Customer changeStatus(UUID customerId, CustomerStatus newStatus) {
        Customer customer = repository.findById(customerId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Customer not found: " + customerId));

        CustomerStatus previousStatus = customer.getStatus();
        customer.transitionTo(newStatus);

        Customer saved = repository.save(customer);
        eventPublisher.publish(
            CustomerStatusChanged.of(saved.getId(), previousStatus, newStatus));
        return saved;
    }

    public Optional<Customer> findById(UUID id) {
        return repository.findById(id);
    }

    public Optional<Customer> findByDocument(Document document) {
        return repository.findByDocument(document);
    }

    public List<Customer> findAll() {
        return repository.findAll();
    }

    private void runValidators(Customer customer) {
        List<String> allErrors = validators.stream()
            .map(v -> v.validate(customer))
            .filter(r -> !r.isValid())
            .flatMap(r -> r.errors().stream())
            .toList();

        if (!allErrors.isEmpty()) {
            throw new CustomerValidationException(allErrors);
        }
    }

    private void checkDuplicate(Document document) {
        if (repository.existsByDocument(document)) {
            throw new DuplicateDocumentException(document.number());
        }
    }

    private Customer runEnrichers(Customer customer) {
        Customer enriched = customer;
        for (CustomerEnricher enricher : enrichers) {
            enriched = enricher.enrich(enriched);
        }
        return enriched;
    }
}
