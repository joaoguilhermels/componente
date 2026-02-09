package com.onefinancial.customer.migration;

/**
 * SPI for JSONB attribute schema migrations.
 *
 * <p>Implementations transform the raw JSON string from one schema version to the next.
 * Migrations are applied sequentially (V1→V2→V3) during the coordinated migration process.</p>
 */
public interface AttributeSchemaMigration {

    /**
     * The schema version this migration reads.
     */
    int sourceVersion();

    /**
     * The schema version this migration produces.
     */
    int targetVersion();

    /**
     * Transforms the JSON from the source version to the target version.
     * Must preserve unknown keys.
     *
     * @param json the raw JSONB string at the source version
     * @return the transformed JSONB string at the target version
     */
    String migrateJson(String json);
}
