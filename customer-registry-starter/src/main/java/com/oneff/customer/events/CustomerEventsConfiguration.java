package com.oneff.customer.events;

import com.oneff.customer.core.port.CustomerEventPublisher;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Bridge configuration exposing the package-private
 * {@link SpringEventPublisherAdapter} for auto-configuration import.
 */
// BRIDGE CONFIG: Auto-config uses @Import(CustomerEventsConfiguration.class)
// to register this bean. @ComponentScan is NOT used (picks up test classes). See ADR-002.
@Configuration(proxyBeanMethods = false)
public class CustomerEventsConfiguration {

    @Bean
    @ConditionalOnMissingBean(CustomerEventPublisher.class)
    public CustomerEventPublisher springEventPublisherAdapter(
            ApplicationEventPublisher applicationEventPublisher) {
        return new SpringEventPublisherAdapter(applicationEventPublisher);
    }
}
