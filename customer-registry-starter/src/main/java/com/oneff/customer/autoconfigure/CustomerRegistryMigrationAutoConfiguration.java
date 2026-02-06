package com.oneff.customer.autoconfigure;

import com.oneff.customer.migration.AttributeMigrationService;
import com.oneff.customer.migration.AttributeSchemaMigration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;

import javax.sql.DataSource;
import java.util.List;

/**
 * Auto-configuration for JSONB attribute schema migration.
 *
 * <p>Activated when {@code customer.registry.features.persistence-jpa=true}.
 * The migration service is always registered when persistence is active
 * (consuming any {@link AttributeSchemaMigration} beans), but it only
 * runs automatically on startup when
 * {@code customer.registry.features.attributes-auto-migrate-on-startup=true}.</p>
 */
@AutoConfiguration(after = CustomerRegistryPersistenceAutoConfiguration.class)
@ConditionalOnProperty(
    prefix = "customer.registry",
    name = {"enabled", "features.persistence-jpa"},
    havingValue = "true"
)
public class CustomerRegistryMigrationAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(
        CustomerRegistryMigrationAutoConfiguration.class);

    @Bean
    public AttributeMigrationService attributeMigrationService(
            DataSource dataSource,
            List<AttributeSchemaMigration> migrations,
            CustomerRegistryProperties properties) {
        return new AttributeMigrationService(
            dataSource,
            migrations,
            properties.getMigration().isStrict()
        );
    }

    @Bean
    @ConditionalOnBean(AttributeMigrationService.class)
    @ConditionalOnProperty(
        name = "customer.registry.features.attributes-auto-migrate-on-startup",
        havingValue = "true"
    )
    AttributeMigrationStartupTrigger attributeMigrationStartupTrigger(
            AttributeMigrationService migrationService) {
        return new AttributeMigrationStartupTrigger(migrationService);
    }

    /**
     * Triggers attribute migration on {@link ApplicationReadyEvent}.
     */
    static class AttributeMigrationStartupTrigger {

        private final AttributeMigrationService migrationService;

        AttributeMigrationStartupTrigger(AttributeMigrationService migrationService) {
            this.migrationService = migrationService;
        }

        @EventListener(ApplicationReadyEvent.class)
        public void onApplicationReady() {
            log.info("Triggering attribute schema migration on startup");
            int result = migrationService.migrate();
            if (result >= 0) {
                log.info("Attribute schema migration completed — {} rows migrated", result);
            } else {
                log.info("Attribute schema migration skipped — lock held by another instance");
            }
        }
    }
}
