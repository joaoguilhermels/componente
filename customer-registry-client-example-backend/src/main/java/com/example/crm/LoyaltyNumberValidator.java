package com.example.crm;

import com.oneff.customer.core.model.Customer;
import com.oneff.customer.core.model.CustomerType;
import com.oneff.customer.core.spi.CustomerValidator;
import com.oneff.customer.core.spi.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Custom validator: PJ customers must have a display name with at least 5 characters.
 * Demonstrates extending the starter via the CustomerValidator SPI.
 */
@Component
public class LoyaltyNumberValidator implements CustomerValidator {

    @Override
    public ValidationResult validate(Customer customer) {
        if (customer.getType() == CustomerType.PJ
                && customer.getDisplayName().length() < 5) {
            return ValidationResult.invalid(
                "example.validation.pj.displayName.tooShort"
            );
        }
        return ValidationResult.valid();
    }
}
