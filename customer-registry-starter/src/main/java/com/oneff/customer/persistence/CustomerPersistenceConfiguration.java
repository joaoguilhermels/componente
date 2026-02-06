package com.oneff.customer.persistence;

import com.oneff.customer.core.port.CustomerRepository;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Module-internal configuration that wires up JPA persistence beans.
 *
 * <p>This class is {@code public} so it can be {@code @Import}ed by the
 * auto-configuration in the {@code autoconfigure} package, but it lives
 * in the {@code persistence} package so it can reference package-private
 * types ({@link CustomerJpaRepository}, {@link CustomerRepositoryJpaAdapter}).</p>
 */
@Configuration(proxyBeanMethods = false)
@EnableJpaRepositories(basePackageClasses = CustomerPersistenceConfiguration.class)
@EntityScan(basePackageClasses = CustomerPersistenceConfiguration.class)
public class CustomerPersistenceConfiguration {

    @Bean
    @ConditionalOnMissingBean(CustomerRepository.class)
    public CustomerRepository customerRepositoryJpaAdapter(CustomerJpaRepository jpaRepository) {
        return new CustomerRepositoryJpaAdapter(jpaRepository);
    }
}
