package com.onefinancial.customer.rest;

import java.util.List;

/**
 * Response DTO representing a paginated list of customers.
 *
 * <p>This record is {@code public} intentionally so that consumers of the library
 * can reference it in their own tests and response handling code.</p>
 *
 * @param customers     the customers on the current page (with masked documents)
 * @param totalElements total number of customers across all pages
 * @param page          zero-based page index
 * @param size          requested page size
 * @param totalPages    total number of pages
 */
public record CustomerPageResponse(
    List<CustomerResponse> customers,
    long totalElements,
    int page,
    int size,
    int totalPages
) {}
