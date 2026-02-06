package com.oneff.customer.persistence;

import com.oneff.customer.core.model.Customer;
import com.oneff.customer.core.model.Document;
import com.oneff.customer.core.port.CustomerRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA adapter implementing the domain's {@link CustomerRepository} port.
 *
 * <p>Translates between the domain {@link Customer} aggregate and the
 * JPA {@link CustomerEntity} via {@link CustomerEntityMapper}.</p>
 */
class CustomerRepositoryJpaAdapter implements CustomerRepository {

    private final CustomerJpaRepository jpaRepository;

    CustomerRepositoryJpaAdapter(CustomerJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Customer save(Customer customer) {
        CustomerEntity entity = CustomerEntityMapper.toEntity(customer);
        CustomerEntity saved = jpaRepository.save(entity);
        return CustomerEntityMapper.toDomain(saved);
    }

    @Override
    public Optional<Customer> findById(UUID id) {
        return jpaRepository.findById(id)
            .map(CustomerEntityMapper::toDomain);
    }

    @Override
    public Optional<Customer> findByDocument(Document document) {
        return jpaRepository.findByDocumentNumber(document.number())
            .map(CustomerEntityMapper::toDomain);
    }

    @Override
    public boolean existsByDocument(Document document) {
        return jpaRepository.existsByDocumentNumber(document.number());
    }

    @Override
    public List<Customer> findAll() {
        return jpaRepository.findAll().stream()
            .map(CustomerEntityMapper::toDomain)
            .toList();
    }
}
