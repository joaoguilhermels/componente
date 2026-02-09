package com.oneff.customer.rest;

import com.oneff.customer.core.service.CustomerRegistryService;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Bridge configuration exposing the package-private REST beans
 * (controller + exception handler) for auto-configuration import.
 */
// BRIDGE CONFIG: Auto-config uses @Import(CustomerRestConfiguration.class)
// to register this bean. @ComponentScan is NOT used (picks up test classes). See ADR-002.
@Configuration(proxyBeanMethods = false)
public class CustomerRestConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "customerController")
    CustomerController customerController(CustomerRegistryService service) {
        return new CustomerController(service);
    }

    @Bean
    @ConditionalOnMissingBean(name = "customerExceptionHandler")
    CustomerExceptionHandler customerExceptionHandler(
            @Qualifier("customerRegistryMessageSource") MessageSource messageSource) {
        return new CustomerExceptionHandler(messageSource);
    }
}
