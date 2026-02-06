package com.oneff.customer.autoconfigure;

import com.oneff.customer.persistence.CustomerPersistenceConfiguration;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import javax.sql.DataSource;

/**
 * Auto-configuration for JPA persistence and Liquibase schema management.
 *
 * <p>Activated when {@code customer.registry.features.persistence-jpa=true}.
 * Imports the JPA entity/repository setup from the persistence module and
 * configures a dedicated Liquibase instance with {@code cr_} prefixed tracking tables.</p>
 */
@AutoConfiguration(after = CustomerRegistryCoreAutoConfiguration.class)
@ConditionalOnProperty(
    prefix = "customer.registry",
    name = {"enabled", "features.persistence-jpa"},
    havingValue = "true"
)
@Import(CustomerPersistenceConfiguration.class)
public class CustomerRegistryPersistenceAutoConfiguration {

    @Bean("customerRegistryLiquibase")
    @ConditionalOnProperty(
        name = "customer.registry.features.migrations",
        havingValue = "true"
    )
    @ConditionalOnClass(SpringLiquibase.class)
    public SpringLiquibase customerRegistryLiquibase(DataSource dataSource) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog("classpath:db/changelog/customer-registry-changelog-master.yaml");
        liquibase.setDatabaseChangeLogTable("cr_databasechangelog");
        liquibase.setDatabaseChangeLogLockTable("cr_databasechangeloglock");
        liquibase.setShouldRun(true);
        return liquibase;
    }
}
