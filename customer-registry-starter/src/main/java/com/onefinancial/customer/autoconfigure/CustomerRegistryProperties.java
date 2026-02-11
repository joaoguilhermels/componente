package com.onefinancial.customer.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Customer Registry starter.
 *
 * <p>Secure-by-default: all features are OFF unless explicitly enabled.
 * <pre>
 * customer:
 *   registry:
 *     enabled: true
 *     features:
 *       rest-api: true
 *       persistence-jpa: true
 *       migrations: true
 *       publish-events: true
 *       attributes-auto-migrate-on-startup: true
 *       observability: true
 *     migration:
 *       advisory-lock-key: 7391825001
 * </pre>
 */
@ConfigurationProperties(prefix = "customer.registry")
public class CustomerRegistryProperties {

    private boolean enabled = false;
    private Features features = new Features();
    private MigrationProperties migration = new MigrationProperties();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Features getFeatures() { return features; }
    public void setFeatures(Features features) { this.features = features; }
    public MigrationProperties getMigration() { return migration; }
    public void setMigration(MigrationProperties migration) { this.migration = migration; }

    public static class Features {
        private boolean restApi = false;
        private boolean persistenceJpa = false;
        private boolean migrations = false;
        private boolean publishEvents = false;
        private boolean attributesAutoMigrateOnStartup = false;
        private boolean observability = false;

        public boolean isRestApi() { return restApi; }
        public void setRestApi(boolean restApi) { this.restApi = restApi; }
        public boolean isPersistenceJpa() { return persistenceJpa; }
        public void setPersistenceJpa(boolean persistenceJpa) { this.persistenceJpa = persistenceJpa; }
        public boolean isMigrations() { return migrations; }
        public void setMigrations(boolean migrations) { this.migrations = migrations; }
        public boolean isPublishEvents() { return publishEvents; }
        public void setPublishEvents(boolean publishEvents) { this.publishEvents = publishEvents; }
        public boolean isAttributesAutoMigrateOnStartup() { return attributesAutoMigrateOnStartup; }
        public void setAttributesAutoMigrateOnStartup(boolean val) { this.attributesAutoMigrateOnStartup = val; }
        public boolean isObservability() { return observability; }
        public void setObservability(boolean observability) { this.observability = observability; }
    }

    public static class MigrationProperties {
        private LockProperties lock = new LockProperties();
        private boolean strict = false;
        private long advisoryLockKey = 7_391_825_001L;

        public LockProperties getLock() { return lock; }
        public void setLock(LockProperties lock) { this.lock = lock; }
        public boolean isStrict() { return strict; }
        public void setStrict(boolean strict) { this.strict = strict; }
        public long getAdvisoryLockKey() { return advisoryLockKey; }
        public void setAdvisoryLockKey(long advisoryLockKey) { this.advisoryLockKey = advisoryLockKey; }
    }

    public static class LockProperties {
        private boolean enabled = false;
        private String scope = "global";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getScope() { return scope; }
        public void setScope(String scope) { this.scope = scope; }
    }
}
