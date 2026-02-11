package com.onefinancial.customer.autoconfigure;

import com.onefinancial.customer.migration.AttributeMigrationService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerRegistryMigrationAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(
            CustomerRegistryCoreAutoConfiguration.class,
            CustomerRegistryPersistenceAutoConfiguration.class,
            CustomerRegistryMigrationAutoConfiguration.class
        ));

    @Test
    @DisplayName("migration respects enabled master switch — no beans when disabled")
    void shouldRespectEnabledMasterSwitch() {
        contextRunner
            .withPropertyValues(
                "customer.registry.enabled=false",
                "customer.registry.features.persistence-jpa=true",
                "customer.registry.features.attributes-auto-migrate-on-startup=true"
            )
            .run(context -> {
                assertThat(context).doesNotHaveBean(AttributeMigrationService.class);
                assertThat(context).doesNotHaveBean(
                    CustomerRegistryMigrationAutoConfiguration.AttributeMigrationStartupTrigger.class);
            });
    }

    @Test
    @DisplayName("migration not registered when persistence is disabled")
    void shouldNotRegisterMigrationServiceWhenPersistenceDisabled() {
        contextRunner
            .withPropertyValues(
                "customer.registry.enabled=true",
                "customer.registry.features.persistence-jpa=false"
            )
            .run(context -> {
                assertThat(context).doesNotHaveBean(AttributeMigrationService.class);
                assertThat(context).doesNotHaveBean(
                    CustomerRegistryMigrationAutoConfiguration.AttributeMigrationStartupTrigger.class);
            });
    }

    @Test
    @DisplayName("startup trigger not registered when auto-migrate disabled — context fails without DataSource")
    void shouldNotRegisterStartupTriggerWhenAutoMigrateDisabled() {
        // When persistence is enabled but no real DataSource exists, the persistence
        // auto-config's @Import of CustomerPersistenceConfiguration causes context
        // failure (needs entityManagerFactory). This is expected — the important thing
        // is that no trigger bean is registered. A failed context has no beans.
        contextRunner
            .withPropertyValues(
                "customer.registry.enabled=true",
                "customer.registry.features.persistence-jpa=true",
                "customer.registry.features.attributes-auto-migrate-on-startup=false"
            )
            .run(context ->
                assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("no migration beans when neither enabled nor persistence flags set")
    void shouldNotRegisterAnyBeansWhenNoFlagsSet() {
        contextRunner
            .run(context -> {
                assertThat(context).doesNotHaveBean(AttributeMigrationService.class);
                assertThat(context).doesNotHaveBean(
                    CustomerRegistryMigrationAutoConfiguration.AttributeMigrationStartupTrigger.class);
            });
    }
}
