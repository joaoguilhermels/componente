package com.oneff.customer.core.spi;

import com.oneff.customer.core.model.Customer;

/**
 * Extension point for custom customer validation logic.
 *
 * <p>Consumers can provide zero or more implementations (e.g., loyalty number validation,
 * tax registration checks). All validators are executed before persistence.</p>
 *
 * <p>Return {@link ValidationResult#valid()} to pass, or
 * {@link ValidationResult#invalid(String)} with a message key to fail.</p>
 */
@FunctionalInterface
public interface CustomerValidator {

    ValidationResult validate(Customer customer);
}
