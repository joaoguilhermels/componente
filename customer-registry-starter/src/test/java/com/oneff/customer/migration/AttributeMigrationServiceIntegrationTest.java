package com.oneff.customer.migration;

import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class AttributeMigrationServiceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("migration_test")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("db/changelog/v1.0.0/001-create-customer-tables.sql");

    private DataSource dataSource;

    @BeforeEach
    void setUp() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(postgres.getJdbcUrl());
        config.setUsername(postgres.getUsername());
        config.setPassword(postgres.getPassword());
        config.setMaximumPoolSize(5);
        dataSource = new HikariDataSource(config);
    }

    @AfterEach
    void tearDown() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM cr_contact");
            stmt.execute("DELETE FROM cr_address");
            stmt.execute("DELETE FROM cr_customer");
        }
        if (dataSource instanceof HikariDataSource hds) {
            hds.close();
        }
    }

    // ─── Helpers ──────────────────────────────────────────────

    private void insertCustomer(UUID id, String docNumber, String json, int schemaVersion)
            throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO cr_customer (id, type, document_number, display_name, status, " +
                 "attributes, schema_version) VALUES (?, 'PF', ?, 'Test', 'DRAFT', ?::jsonb, ?)")) {
            ps.setObject(1, id);
            ps.setString(2, docNumber);
            ps.setString(3, json);
            ps.setInt(4, schemaVersion);
            ps.executeUpdate();
        }
    }

    private String readAttributes(UUID id) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT attributes FROM cr_customer WHERE id = ?")) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getString(1);
            }
        }
    }

    private int readSchemaVersion(UUID id) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT schema_version FROM cr_customer WHERE id = ?")) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    // ─── Migrations ───────────────────────────────────────────

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static AttributeSchemaMigration v1ToV2() {
        return new AttributeSchemaMigration() {
            @Override public int sourceVersion() { return 1; }
            @Override public int targetVersion() { return 2; }
            @Override
            public String migrateJson(String json) {
                try {
                    Map<String, Object> map = MAPPER.readValue(json,
                        new TypeReference<LinkedHashMap<String, Object>>() {});
                    map.put("schemaVersion", 2);
                    map.put("migratedV1toV2", true);
                    return MAPPER.writeValueAsString(map);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    private static AttributeSchemaMigration v2ToV3() {
        return new AttributeSchemaMigration() {
            @Override public int sourceVersion() { return 2; }
            @Override public int targetVersion() { return 3; }
            @Override
            public String migrateJson(String json) {
                try {
                    Map<String, Object> map = MAPPER.readValue(json,
                        new TypeReference<LinkedHashMap<String, Object>>() {});
                    map.put("schemaVersion", 3);
                    map.put("migratedV2toV3", true);
                    return MAPPER.writeValueAsString(map);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    // ─── Tests ────────────────────────────────────────────────

    @Test
    @DisplayName("should migrate V1 rows to V2")
    void migratesV1ToV2() throws SQLException {
        UUID id = UUID.randomUUID();
        insertCustomer(id, "52998224725",
            "{\"schemaVersion\":1,\"values\":{}}", 1);

        var service = new AttributeMigrationService(
            dataSource, List.of(v1ToV2()), false);
        int migrated = service.migrate();

        assertThat(migrated).isEqualTo(1);
        assertThat(readSchemaVersion(id)).isEqualTo(2);
        assertThat(readAttributes(id)).contains("\"migratedV1toV2\": true");
    }

    @Test
    @DisplayName("should migrate V1→V2→V3 sequentially")
    void migratesSequentially() throws SQLException {
        UUID id = UUID.randomUUID();
        insertCustomer(id, "52998224725",
            "{\"schemaVersion\":1,\"values\":{}}", 1);

        var service = new AttributeMigrationService(
            dataSource, List.of(v2ToV3(), v1ToV2()), false); // Intentionally out of order
        int migrated = service.migrate();

        assertThat(migrated).isEqualTo(2); // 1 row migrated in V1→V2, same row again in V2→V3
        assertThat(readSchemaVersion(id)).isEqualTo(3);
        String attrs = readAttributes(id);
        assertThat(attrs).contains("\"migratedV1toV2\": true");
        assertThat(attrs).contains("\"migratedV2toV3\": true");
    }

    @Test
    @DisplayName("should skip rows already at target version")
    void skipsAlreadyMigratedRows() throws SQLException {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        insertCustomer(id1, "52998224725",
            "{\"schemaVersion\":1,\"values\":{}}", 1);
        insertCustomer(id2, "11222333000181",
            "{\"schemaVersion\":2,\"values\":{}}", 2);

        var service = new AttributeMigrationService(
            dataSource, List.of(v1ToV2()), false);
        int migrated = service.migrate();

        assertThat(migrated).isEqualTo(1);
        assertThat(readSchemaVersion(id1)).isEqualTo(2);
        assertThat(readSchemaVersion(id2)).isEqualTo(2); // unchanged
    }

    @Test
    @DisplayName("should be idempotent — second run is no-op")
    void idempotentMigration() throws SQLException {
        UUID id = UUID.randomUUID();
        insertCustomer(id, "52998224725",
            "{\"schemaVersion\":1,\"values\":{}}", 1);

        var service = new AttributeMigrationService(
            dataSource, List.of(v1ToV2()), false);
        int first = service.migrate();
        int second = service.migrate();

        assertThat(first).isEqualTo(1);
        assertThat(second).isEqualTo(0);
    }

    @Test
    @DisplayName("should preserve unknown keys during migration")
    void preservesUnknownKeys() throws SQLException {
        UUID id = UUID.randomUUID();
        String json = "{\"schemaVersion\":1,\"values\":{\"customField\":{\"type\":\"STRING\",\"value\":\"keep-me\"}}}";
        insertCustomer(id, "52998224725", json, 1);

        var service = new AttributeMigrationService(
            dataSource, List.of(v1ToV2()), false);
        service.migrate();

        String result = readAttributes(id);
        assertThat(result).contains("\"customField\"");
        assertThat(result).contains("\"keep-me\"");
    }

    @Test
    @DisplayName("should return 0 when no migrations are registered")
    void returnsZeroWithNoMigrations() {
        var service = new AttributeMigrationService(
            dataSource, List.of(), false);
        int migrated = service.migrate();

        assertThat(migrated).isEqualTo(0);
    }

    @Test
    @DisplayName("advisory lock prevents concurrent migration — second caller gets -1")
    void advisoryLockPreventsConcurrent() throws Exception {
        UUID id = UUID.randomUUID();
        insertCustomer(id, "52998224725",
            "{\"schemaVersion\":1,\"values\":{}}", 1);

        // Slow migration that holds the lock for a noticeable duration
        AttributeSchemaMigration slowMigration = new AttributeSchemaMigration() {
            @Override public int sourceVersion() { return 1; }
            @Override public int targetVersion() { return 2; }
            @Override
            public String migrateJson(String json) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return json.replace("\"schemaVersion\":1", "\"schemaVersion\":2");
            }
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            var service1 = new AttributeMigrationService(
                dataSource, List.of(slowMigration), false);
            var service2 = new AttributeMigrationService(
                dataSource, List.of(v1ToV2()), false);

            Future<Integer> future1 = executor.submit(service1::migrate);
            Thread.sleep(100); // Let service1 acquire lock first
            Future<Integer> future2 = executor.submit(service2::migrate);

            int result1 = future1.get(5, TimeUnit.SECONDS);
            int result2 = future2.get(5, TimeUnit.SECONDS);

            // One should succeed (1 row), the other should be skipped (-1)
            assertThat(List.of(result1, result2)).containsExactlyInAnyOrder(1, -1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("strict mode throws when lock is held by another session")
    void strictModeThrows() throws Exception {
        UUID id = UUID.randomUUID();
        insertCustomer(id, "52998224725",
            "{\"schemaVersion\":1,\"values\":{}}", 1);

        // Hold advisory lock manually on a separate connection
        try (Connection lockConn = dataSource.getConnection();
             PreparedStatement ps = lockConn.prepareStatement(
                 "SELECT pg_try_advisory_lock(?)")) {
            ps.setLong(1, PostgresAdvisoryLock.LOCK_KEY);
            ps.execute();

            var service = new AttributeMigrationService(
                dataSource, List.of(v1ToV2()), true);

            assertThatThrownBy(service::migrate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("strict mode");

            // Release lock
            try (PreparedStatement unlock = lockConn.prepareStatement(
                     "SELECT pg_advisory_unlock(?)")) {
                unlock.setLong(1, PostgresAdvisoryLock.LOCK_KEY);
                unlock.execute();
            }
        }
    }
}
