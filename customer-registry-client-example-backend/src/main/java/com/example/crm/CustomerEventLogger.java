package com.example.crm;

import com.oneff.customer.core.event.CustomerCreated;
import com.oneff.customer.core.event.CustomerStatusChanged;
import com.oneff.customer.core.event.CustomerUpdated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listens to customer domain events published by the starter.
 * Demonstrates how host apps can react to lifecycle events.
 */
@Component
public class CustomerEventLogger {

    private static final Logger log = LoggerFactory.getLogger(CustomerEventLogger.class);

    @EventListener
    public void onCustomerCreated(CustomerCreated event) {
        log.info("[ExampleCRM] Customer created: id={}, type={}",
            event.customerId(), event.customerType());
    }

    @EventListener
    public void onCustomerUpdated(CustomerUpdated event) {
        log.info("[ExampleCRM] Customer updated: id={}", event.customerId());
    }

    @EventListener
    public void onStatusChanged(CustomerStatusChanged event) {
        log.info("[ExampleCRM] Customer status changed: id={}, {} -> {}",
            event.customerId(), event.fromStatus(), event.toStatus());
    }
}
