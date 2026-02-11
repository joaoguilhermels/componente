package com.onefinancial.customer.autoconfigure;

import com.onefinancial.customer.core.port.CustomerEventPublisher;
import com.onefinancial.customer.core.port.CustomerRepository;
import com.onefinancial.customer.core.service.CustomerRegistryService;
import com.onefinancial.customer.core.spi.CustomerEnricher;
import com.onefinancial.customer.core.spi.CustomerValidator;
import com.onefinancial.customer.core.spi.CustomerOperationMetrics;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import java.util.List;
import java.util.Optional;

/**
 * Core auto-configuration for Customer Registry.
 *
 * <p>Gated by {@code customer.registry.enabled=true}. Registers the domain service,
 * fallback repository (in-memory), and fallback event publisher (no-op).
 * All beans use {@code @ConditionalOnMissingBean} so consumers can override.</p>
 *
 * <p>ORDERING: Default (no explicit ordering). Other auto-configs (events) run before this.
 * GATE: customer.registry.enabled=true (master switch).
 * OVERRIDABLE: All beans use @ConditionalOnMissingBean — host app can replace repository,
 *              event publisher, or the entire service.
 * FALLBACKS: InMemoryCustomerRepository (no DB needed), NoOpEventPublisher (events disabled).
 * See ADR-003 for rationale.</p>
 */
@AutoConfiguration
@ConditionalOnProperty(name = "customer.registry.enabled", havingValue = "true")
@EnableConfigurationProperties(CustomerRegistryProperties.class)
public class CustomerRegistryCoreAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CustomerRepository customerRepository() {
        return new InMemoryCustomerRepository();
    }

    @Bean
    @ConditionalOnMissingBean
    public CustomerEventPublisher customerEventPublisher() {
        return new NoOpEventPublisher();
    }

    @Bean
    @ConditionalOnMissingBean
    public CustomerRegistryService customerRegistryService(
            List<CustomerValidator> validators,
            List<CustomerEnricher> enrichers,
            CustomerRepository repository,
            CustomerEventPublisher eventPublisher,
            Optional<CustomerOperationMetrics> metrics) {
        return new CustomerRegistryService(validators, enrichers, repository, eventPublisher, metrics);
    }

    @Bean
    @ConditionalOnMissingBean
    public CustomerOperationMetrics customerOperationMetrics() {
        return new NoOpOperationMetrics();
    }

    @Bean("customerRegistryMessageSource")
    @ConditionalOnMissingBean(name = "customerRegistryMessageSource")
    public MessageSource customerRegistryMessageSource() {
        ReloadableResourceBundleMessageSource messageSource =
            new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:messages/customer-registry-messages");
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource;
    }
}
