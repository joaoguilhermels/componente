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
@Configuration(proxyBeanMethods = false)
public class CustomerEventsConfiguration {

    @Bean
    @ConditionalOnMissingBean(CustomerEventPublisher.class)
    public CustomerEventPublisher springEventPublisherAdapter(
            ApplicationEventPublisher applicationEventPublisher) {
        return new SpringEventPublisherAdapter(applicationEventPublisher);
    }
}
