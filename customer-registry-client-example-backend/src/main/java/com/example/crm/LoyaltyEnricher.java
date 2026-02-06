package com.example.crm;

import com.oneff.customer.core.model.AttributeValue.StringValue;
import com.oneff.customer.core.model.Customer;
import com.oneff.customer.core.spi.CustomerEnricher;
import org.springframework.stereotype.Component;

/**
 * Custom enricher: assigns a default loyalty tier to new customers.
 * Demonstrates extending the starter via the CustomerEnricher SPI.
 */
@Component
public class LoyaltyEnricher implements CustomerEnricher {

    private static final String LOYALTY_TIER_KEY = "loyaltyTier";
    private static final String DEFAULT_TIER = "BRONZE";

    @Override
    public Customer enrich(Customer customer) {
        if (!customer.getAttributes().containsKey(LOYALTY_TIER_KEY)) {
            customer.setAttributes(
                customer.getAttributes().with(LOYALTY_TIER_KEY, new StringValue(DEFAULT_TIER))
            );
        }
        return customer;
    }
}
