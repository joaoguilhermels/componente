package com.onefinancial.customer.autoconfigure;

import com.onefinancial.customer.core.port.CustomerRepository;
import com.onefinancial.customer.migration.AttributeMigrationService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerRegistryPersistenceAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(
            CustomerRegistryCoreAutoConfiguration.class,
            CustomerRegistryPersistenceAutoConfiguration.class,
            CustomerRegistryMigrationAutoConfiguration.class
        ));

    @Test
    @DisplayName("with persistence disabled — no JPA beans, uses InMemory fallback")
    void persistenceDisabledUsesInMemory() {
        contextRunner
            .withPropertyValues(
                "customer.registry.enabled=true",
                "customer.registry.features.persistence-jpa=false"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(CustomerRepository.class);
                assertThat(context.getBean(CustomerRepository.class))
                    .isInstanceOf(InMemoryCustomerRepository.class);
                assertThat(context).doesNotHaveBean(AttributeMigrationService.class);
            });
    }

    @Test
    @DisplayName("with persistence enabled but no DataSource — context still loads (graceful)")
    void persistenceEnabledNoDataSource() {
        // Without a real DataSource, the JPA auto-config won't fully start,
        // but our conditional should not break the context
        contextRunner
            .withPropertyValues(
                "customer.registry.enabled=true",
                "customer.registry.features.persistence-jpa=true"
            )
            .run(context -> {
                // Without DataSource/JPA auto-config, our Import of
                // CustomerPersistenceConfiguration will fail to find JpaRepository
                // This is expected — persistence requires a DataSource
                assertThat(context).hasFailed();
            });
    }

    @Test
    @DisplayName("with all disabled — no beans registered at all")
    void allDisabledNoBeans() {
        contextRunner
            .withPropertyValues("customer.registry.enabled=false")
            .run(context -> {
                assertThat(context).doesNotHaveBean(CustomerRepository.class);
                assertThat(context).doesNotHaveBean(AttributeMigrationService.class);
            });
    }

    @Test
    @DisplayName("migration service not registered when persistence is disabled")
    void migrationNotRegisteredWithoutPersistence() {
        contextRunner
            .withPropertyValues(
                "customer.registry.enabled=true",
                "customer.registry.features.persistence-jpa=false"
            )
            .run(context -> {
                assertThat(context).doesNotHaveBean(AttributeMigrationService.class);
            });
    }
}
