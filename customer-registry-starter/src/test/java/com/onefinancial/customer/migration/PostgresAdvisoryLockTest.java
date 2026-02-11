package com.onefinancial.customer.migration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class PostgresAdvisoryLockTest {

    @Test
    @DisplayName("should use default lock key when none specified")
    void shouldUseDefaultLockKey() {
        assertThat(PostgresAdvisoryLock.DEFAULT_LOCK_KEY).isEqualTo(7_391_825_001L);
    }

    @Test
    @DisplayName("should accept custom lock key via constructor")
    void shouldAcceptCustomLockKey() {
        PostgresAdvisoryLock lock = new PostgresAdvisoryLock(null, 12345L);
        assertThat(lock.getLockKey()).isEqualTo(12345L);
    }

    @Test
    @DisplayName("should use default lock key when using single-arg constructor")
    void shouldUseDefaultLockKeyWithSingleArgConstructor() {
        PostgresAdvisoryLock lock = new PostgresAdvisoryLock(null);
        assertThat(lock.getLockKey()).isEqualTo(PostgresAdvisoryLock.DEFAULT_LOCK_KEY);
    }

    @Test
    @DisplayName("should close connection when setAutoCommit throws")
    void shouldCloseConnectionWhenSetAutoCommitThrows() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        doThrow(new SQLException("setAutoCommit failed")).when(connection).setAutoCommit(true);

        PostgresAdvisoryLock lock = new PostgresAdvisoryLock(dataSource);

        assertThatThrownBy(lock::tryAcquire)
            .isInstanceOf(SQLException.class)
            .hasMessageContaining("setAutoCommit failed");

        verify(connection).close();
    }

    @Test
    @DisplayName("close() should close dedicated connection even when lock was not acquired")
    void closeShouldCloseConnectionWhenNotAcquired() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getBoolean(1)).thenReturn(false); // lock NOT acquired

        PostgresAdvisoryLock lock = new PostgresAdvisoryLock(dataSource);
        boolean acquired = lock.tryAcquire();

        assertThat(acquired).isFalse();

        // Connection should already be closed by tryAcquire when not acquired
        verify(connection).close();

        // Calling close() again should not throw
        lock.close();
    }

    @Test
    @DisplayName("close() should close connection when prepareStatement fails after connection assigned")
    void closeShouldCloseConnectionWhenPrepareStatementFails() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);

        when(dataSource.getConnection()).thenReturn(connection);
        // setAutoCommit succeeds, but prepareStatement fails
        when(connection.prepareStatement(anyString()))
            .thenThrow(new SQLException("prepareStatement failed"));

        PostgresAdvisoryLock lock = new PostgresAdvisoryLock(dataSource);

        assertThatThrownBy(lock::tryAcquire)
            .isInstanceOf(SQLException.class)
            .hasMessageContaining("prepareStatement failed");

        // acquired is false, but dedicatedConnection was assigned
        assertThat(lock.isAcquired()).isFalse();

        // close() must still close the dedicated connection
        lock.close();
        verify(connection).close();
    }
}
