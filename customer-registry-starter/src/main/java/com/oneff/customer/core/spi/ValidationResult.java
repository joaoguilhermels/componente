package com.oneff.customer.core.spi;

import java.util.Collections;
import java.util.List;

/**
 * Result of a customer validation step. Either valid (no errors)
 * or invalid (one or more error message keys).
 *
 * @param isValid whether this result indicates a passing validation
 * @param errors  list of error message keys (empty when valid)
 */
public record ValidationResult(boolean isValid, List<String> errors) {

    private static final ValidationResult VALID = new ValidationResult(true, List.of());

    public static ValidationResult valid() {
        return VALID;
    }

    public static ValidationResult invalid(String errorMessageKey) {
        return new ValidationResult(false, List.of(errorMessageKey));
    }

    public static ValidationResult invalid(List<String> errorMessageKeys) {
        return new ValidationResult(false, Collections.unmodifiableList(errorMessageKeys));
    }
}
