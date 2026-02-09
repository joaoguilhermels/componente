package com.oneff.customer.core.port;

import com.oneff.customer.core.model.Customer;
import com.oneff.customer.core.model.CustomerPage;
import com.oneff.customer.core.model.Document;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for customer persistence operations.
 *
 * <p>Implementations may use JPA, in-memory storage, or any other persistence mechanism.
 * The core domain depends only on this interface, never on a concrete adapter.</p>
 */
public interface CustomerRepository {

    Customer save(Customer customer);

    Optional<Customer> findById(UUID id);

    Optional<Customer> findByDocument(Document document);

    boolean existsByDocument(Document document);

    /**
     * @deprecated Use {@link #findAll(int, int)} for paginated results.
     */
    @Deprecated
    List<Customer> findAll();

    CustomerPage findAll(int page, int size);

    void deleteById(UUID id);
}
