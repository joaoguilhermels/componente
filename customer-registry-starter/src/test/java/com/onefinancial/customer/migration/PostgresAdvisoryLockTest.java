package com.onefinancial.customer.migration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
}
