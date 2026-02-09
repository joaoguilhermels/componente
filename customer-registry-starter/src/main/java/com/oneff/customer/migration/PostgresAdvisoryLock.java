package com.oneff.customer.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * AutoCloseable wrapper around PostgreSQL session-level advisory locks.
 *
 * <p>Uses a <strong>dedicated JDBC connection</strong> (not from the pool) to hold
 * the lock for the entire migration duration. Advisory locks are session-scoped,
 * so returning the connection to the pool would release the lock prematurely.</p>
 *
 * <p>The lock is non-blocking: {@link #tryAcquire()} returns immediately with
 * {@code false} if another node already holds the lock.</p>
 *
 * <p><strong>Connection pool requirement:</strong> Because this class acquires a
 * dedicated JDBC connection separate from the one used by the migration queries,
 * the connection pool must be configured with a <em>minimum size of 2</em>.
 * If the pool has only 1 connection, the migration transaction will deadlock
 * waiting for a connection while this lock holds the only one.</p>
 */
public class PostgresAdvisoryLock implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(PostgresAdvisoryLock.class);

    /**
     * Lock key derived from "customer-registry-migration".hashCode() to avoid collisions.
     */
    static final long LOCK_KEY = 7_391_825_001L;

    private final DataSource dataSource;
    private Connection dedicatedConnection;
    private boolean acquired;

    public PostgresAdvisoryLock(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Attempts to acquire the advisory lock without blocking.
     *
     * @return {@code true} if the lock was acquired, {@code false} if another session holds it
     * @throws SQLException if a database error occurs
     */
    public boolean tryAcquire() throws SQLException {
        dedicatedConnection = dataSource.getConnection();
        dedicatedConnection.setAutoCommit(true);

        try (PreparedStatement ps = dedicatedConnection
                .prepareStatement("SELECT pg_try_advisory_lock(?)")) {
            ps.setLong(1, LOCK_KEY);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                acquired = rs.getBoolean(1);
            }
        }

        if (acquired) {
            log.info("Acquired advisory lock (key={}) for attribute schema migration", LOCK_KEY);
        } else {
            log.info("Advisory lock (key={}) already held by another session — skipping migration", LOCK_KEY);
            closeDedicatedConnection();
        }
        return acquired;
    }

    /**
     * Returns whether this instance currently holds the lock.
     */
    public boolean isAcquired() {
        return acquired;
    }

    /**
     * Releases the advisory lock and closes the dedicated connection.
     */
    @Override
    public void close() {
        if (!acquired || dedicatedConnection == null) {
            return;
        }
        try (PreparedStatement ps = dedicatedConnection
                .prepareStatement("SELECT pg_advisory_unlock(?)")) {
            ps.setLong(1, LOCK_KEY);
            ps.execute();
            log.info("Released advisory lock (key={})", LOCK_KEY);
        } catch (SQLException e) {
            log.warn("Failed to release advisory lock (key={}): {}", LOCK_KEY, e.getMessage());
        } finally {
            acquired = false;
            closeDedicatedConnection();
        }
    }

    private void closeDedicatedConnection() {
        if (dedicatedConnection != null) {
            try {
                dedicatedConnection.close();
            } catch (SQLException e) {
                log.warn("Failed to close dedicated lock connection: {}", e.getMessage());
            }
            dedicatedConnection = null;
        }
    }
}
