package com.onefinancial.customer.core.model;

import java.util.List;

/**
 * A page of customer results for paginated queries.
 *
 * @param customers     the customers in this page
 * @param totalElements total number of customers across all pages
 * @param page          zero-based page index
 * @param size          maximum number of customers per page
 */
public record CustomerPage(
    List<Customer> customers,
    long totalElements,
    int page,
    int size
) {
    public CustomerPage {
        java.util.Objects.requireNonNull(customers, "customers must not be null");
        if (totalElements < 0) {
            throw new IllegalArgumentException("totalElements must be >= 0, got: " + totalElements);
        }
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0, got: " + page);
        }
        if (size <= 0) {
            throw new IllegalArgumentException("size must be > 0, got: " + size);
        }
    }
}
