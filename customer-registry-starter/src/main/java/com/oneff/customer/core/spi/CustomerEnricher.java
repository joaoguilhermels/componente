package com.oneff.customer.core.spi;

import com.oneff.customer.core.model.Customer;

/**
 * Extension point for enriching customer data before persistence.
 *
 * <p>Consumers can provide zero or more implementations (e.g., populating
 * loyalty attributes from an external CRM, setting default contact preferences).
 * All enrichers run after validation passes.</p>
 */
@FunctionalInterface
public interface CustomerEnricher {

    Customer enrich(Customer customer);
}
