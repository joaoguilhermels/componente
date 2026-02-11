package com.onefinancial.customer.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Comparator;
import java.util.List;

/**
 * Coordinates JSONB attribute schema migrations across cluster nodes.
 *
 * <p>On application startup (when triggered), this service:
 * <ol>
 *   <li>Acquires a PostgreSQL advisory lock (non-blocking)</li>
 *   <li>Reads the minimum {@code schema_version} from {@code cr_customer}</li>
 *   <li>Applies registered {@link AttributeSchemaMigration}s sequentially (V1&rarr;V2&rarr;V3)</li>
 *   <li>Batch-updates the JSONB column and {@code schema_version} for affected rows</li>
 *   <li>Releases the lock</li>
 * </ol>
 *
 * <p>If the lock is already held by another node, migration is skipped
 * unless {@code strict} mode is enabled (which throws an exception).</p>
 *
 * <p><strong>Concurrency constraint:</strong> Attribute schema migrations operate
 * directly on the database via JDBC, bypassing JPA entirely. They must <em>not</em>
 * run concurrently with JPA operations on the same rows, as JPA's first-level cache
 * would become stale. In practice, this means migrations should run during application
 * startup (before the JPA {@code EntityManager} serves requests) or during a
 * maintenance window.</p>
 */
public class AttributeMigrationService {

    private static final Logger log = LoggerFactory.getLogger(AttributeMigrationService.class);

    private static final String SELECT_MIN_VERSION =
        "SELECT COALESCE(MIN(schema_version), 1) FROM cr_customer";

    private static final String SELECT_ROWS_AT_VERSION =
        "SELECT id, attributes FROM cr_customer WHERE schema_version = ?";

    private static final String UPDATE_ROW =
        "UPDATE cr_customer SET attributes = ?::jsonb, schema_version = ?, updated_at = NOW() WHERE id = ?";

    private final DataSource dataSource;
    private final List<AttributeSchemaMigration> migrations;
    private final boolean strict;
    private final long advisoryLockKey;

    public AttributeMigrationService(
            DataSource dataSource,
            List<AttributeSchemaMigration> migrations,
            boolean strict) {
        this(dataSource, migrations, strict, PostgresAdvisoryLock.DEFAULT_LOCK_KEY);
    }

    public AttributeMigrationService(
            DataSource dataSource,
            List<AttributeSchemaMigration> migrations,
            boolean strict,
            long advisoryLockKey) {
        this.dataSource = dataSource;
        this.migrations = migrations.stream()
            .sorted(Comparator.comparingInt(AttributeSchemaMigration::sourceVersion))
            .toList();
        this.strict = strict;
        this.advisoryLockKey = advisoryLockKey;
    }

    /**
     * Executes the migration process with advisory lock coordination.
     *
     * @return the number of rows migrated, or -1 if the lock was not acquired
     */
    public int migrate() {
        if (migrations.isEmpty()) {
            log.info("No attribute schema migrations registered — skipping");
            return 0;
        }

        try (PostgresAdvisoryLock lock = new PostgresAdvisoryLock(dataSource, advisoryLockKey)) {
            if (!lock.tryAcquire()) {
                if (strict) {
                    throw new IllegalStateException(
                        "Attribute schema migration lock is held by another session and strict mode is enabled");
                }
                return -1;
            }
            return executeMigrations();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed during attribute schema migration", e);
        }
    }

    private int executeMigrations() throws SQLException {
        int totalMigrated = 0;

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            int currentMinVersion = readMinVersion(conn);
            log.info("Current minimum attribute schema version: {}", currentMinVersion);

            for (AttributeSchemaMigration migration : migrations) {
                if (migration.sourceVersion() < currentMinVersion) {
                    log.debug("Skipping migration V{} → V{} (already past source version)",
                        migration.sourceVersion(), migration.targetVersion());
                    continue;
                }

                int migrated = applyMigration(conn, migration);
                totalMigrated += migrated;
                log.info("Migrated {} rows from V{} → V{}",
                    migrated, migration.sourceVersion(), migration.targetVersion());
            }

            conn.commit();
        }

        log.info("Attribute schema migration complete — {} total rows migrated", totalMigrated);
        return totalMigrated;
    }

    private int readMinVersion(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT_MIN_VERSION);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private int applyMigration(Connection conn, AttributeSchemaMigration migration)
            throws SQLException {
        int count = 0;

        try (PreparedStatement select = conn.prepareStatement(SELECT_ROWS_AT_VERSION);
             PreparedStatement update = conn.prepareStatement(UPDATE_ROW)) {

            select.setInt(1, migration.sourceVersion());

            try (ResultSet rs = select.executeQuery()) {
                while (rs.next()) {
                    java.util.UUID id = (java.util.UUID) rs.getObject("id");
                    String json = rs.getString("attributes");

                    String migratedJson = migration.migrateJson(json);

                    update.setString(1, migratedJson);
                    update.setInt(2, migration.targetVersion());
                    update.setObject(3, id);
                    update.addBatch();
                    count++;
                }
            }

            if (count > 0) {
                update.executeBatch();
            }
        }
        return count;
    }
}
