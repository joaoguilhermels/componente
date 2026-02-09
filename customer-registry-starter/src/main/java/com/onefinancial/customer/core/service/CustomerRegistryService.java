package com.onefinancial.customer.core.service;

import com.onefinancial.customer.core.event.CustomerCreated;
import com.onefinancial.customer.core.event.CustomerDeleted;
import com.onefinancial.customer.core.event.CustomerStatusChanged;
import com.onefinancial.customer.core.event.CustomerUpdated;
import com.onefinancial.customer.core.exception.CustomerNotFoundException;
import com.onefinancial.customer.core.exception.CustomerValidationException;
import com.onefinancial.customer.core.exception.DuplicateDocumentException;
import com.onefinancial.customer.core.model.Customer;
import com.onefinancial.customer.core.model.CustomerPage;
import com.onefinancial.customer.core.model.CustomerStatus;
import com.onefinancial.customer.core.model.Document;
import com.onefinancial.customer.core.port.CustomerEventPublisher;
import com.onefinancial.customer.core.port.CustomerRepository;
import com.onefinancial.customer.core.spi.CustomerEnricher;
import com.onefinancial.customer.core.spi.CustomerValidator;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Core domain service orchestrating the customer registration pipeline.
 *
 * <p>Pipeline: validate all → check duplicate → enrich all → persist → publish event.
 *
 * <p>This service has no Spring stereotype annotations by design — it's wired by
 * the auto-configuration layer, allowing it to be tested in isolation.
 * Transaction boundaries are applied via Spring AOP proxying.</p>
 */
// Architecture note: No @Service annotation — wired by auto-config
// (@ConditionalOnMissingBean) so host apps can provide an alternative implementation.
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
    @Transactional
    public Customer register(Customer customer) {
        runValidators(customer);
        checkDuplicate(customer.getDocument());
        customer = runEnrichers(customer);
        Customer saved = repository.save(customer);
        eventPublisher.publish(CustomerCreated.of(saved.getId(), saved.getType()));
        return saved;
    }

    /**
     * Updates an existing customer's data.
     */
    @Transactional
    public Customer update(Customer customer) {
        runValidators(customer);
        Customer saved = repository.save(customer);
        eventPublisher.publish(CustomerUpdated.of(saved.getId()));
        return saved;
    }

    /**
     * Transitions a customer to a new lifecycle status.
     *
     * @throws CustomerNotFoundException if the customer does not exist
     */
    @Transactional
    public Customer changeStatus(UUID customerId, CustomerStatus newStatus) {
        Customer customer = repository.findById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException(customerId));

        CustomerStatus previousStatus = customer.getStatus();
        customer.transitionTo(newStatus);

        Customer saved = repository.save(customer);
        eventPublisher.publish(
            CustomerStatusChanged.of(saved.getId(), previousStatus, newStatus));
        return saved;
    }

    /**
     * Deletes a customer by ID.
     *
     * @throws CustomerNotFoundException if the customer does not exist
     */
    @Transactional
    public void deleteCustomer(UUID customerId) {
        repository.findById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException(customerId));
        repository.deleteById(customerId);
        eventPublisher.publish(CustomerDeleted.of(customerId));
    }

    @Transactional(readOnly = true)
    public Optional<Customer> findById(UUID id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Customer> findByDocument(Document document) {
        return repository.findByDocument(document);
    }

    @Transactional(readOnly = true)
    public List<Customer> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public CustomerPage findAllPaginated(int page, int size) {
        return repository.findAll(page, size);
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
            throw new DuplicateDocumentException();
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
