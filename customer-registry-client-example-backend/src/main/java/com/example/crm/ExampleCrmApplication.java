package com.example.crm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Example application demonstrating how to consume the Customer Registry Starter.
 *
 * <p>This app enables all starter features and extends it with:
 * <ul>
 *   <li>LoyaltyNumberValidator — validates that PJ customers have a loyalty number attribute</li>
 *   <li>LoyaltyEnricher — enriches new customers with a default loyalty tier</li>
 *   <li>CustomerEventLogger — listens to customer domain events</li>
 * </ul>
 */
@SpringBootApplication
public class ExampleCrmApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExampleCrmApplication.class, args);
    }
}
