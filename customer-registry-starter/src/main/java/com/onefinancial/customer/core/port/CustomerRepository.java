package com.onefinancial.customer.core.port;

import com.onefinancial.customer.core.model.Customer;
import com.onefinancial.customer.core.model.CustomerPage;
import com.onefinancial.customer.core.model.Document;

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
     * This method loads ALL customers into memory and may cause OutOfMemoryError
     * on datasets with more than ~100k rows. Will be removed in a future version.
     */
    @Deprecated
    default List<Customer> findAll() {
        return findAll(0, Integer.MAX_VALUE).customers();
    }

    /**
     * Returns a page of customers.
     *
     * @param page zero-based page index, must be {@code >= 0}
     * @param size maximum number of customers per page, must be {@code > 0}
     * @return a page of customers, never null
     */
    CustomerPage findAll(int page, int size);

    void deleteById(UUID id);
}
