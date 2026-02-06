package com.oneff.customer.autoconfigure;

import com.oneff.customer.core.model.Customer;
import com.oneff.customer.core.model.Document;
import com.oneff.customer.core.port.CustomerRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of {@link CustomerRepository}.
 *
 * <p>Used as a fallback when JPA persistence is not enabled.
 * Useful for testing and prototyping.</p>
 */
class InMemoryCustomerRepository implements CustomerRepository {

    private final Map<UUID, Customer> store = new ConcurrentHashMap<>();

    @Override
    public Customer save(Customer customer) {
        store.put(customer.getId(), customer);
        return customer;
    }

    @Override
    public Optional<Customer> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<Customer> findByDocument(Document document) {
        return store.values().stream()
            .filter(c -> c.getDocument().equals(document))
            .findFirst();
    }

    @Override
    public boolean existsByDocument(Document document) {
        return store.values().stream()
            .anyMatch(c -> c.getDocument().equals(document));
    }

    @Override
    public List<Customer> findAll() {
        return new ArrayList<>(store.values());
    }
}
