package com.oneff.customer.core.model;

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
}
