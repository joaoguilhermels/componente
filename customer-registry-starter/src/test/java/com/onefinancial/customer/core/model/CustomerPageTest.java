package com.onefinancial.customer.core.model;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerPageTest {

    private static final String VALID_CPF = "52998224725";

    @Test
    void shouldCreateValidPage() {
        var customer = Customer.createPF(VALID_CPF, "João Silva");
        var page = new CustomerPage(List.of(customer), 1L, 0, 10);

        assertThat(page.customers()).hasSize(1);
        assertThat(page.totalElements()).isEqualTo(1L);
        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(10);
    }

    @Test
    void shouldAcceptEmptyCustomerList() {
        var page = new CustomerPage(Collections.emptyList(), 0L, 0, 10);

        assertThat(page.customers()).isEmpty();
        assertThat(page.totalElements()).isZero();
    }

    @Test
    void shouldRejectNullCustomerList() {
        assertThatThrownBy(() -> new CustomerPage(null, 0L, 0, 10))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("customers must not be null");
    }

    @Test
    void shouldRejectNegativeTotalElements() {
        assertThatThrownBy(() -> new CustomerPage(List.of(), -1L, 0, 10))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("totalElements must be >= 0");
    }

    @Test
    void shouldRejectNegativePage() {
        assertThatThrownBy(() -> new CustomerPage(List.of(), 0L, -1, 10))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("page must be >= 0");
    }

    @Test
    void shouldRejectZeroSize() {
        assertThatThrownBy(() -> new CustomerPage(List.of(), 0L, 0, 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("size must be > 0");
    }

    @Test
    void shouldRejectNegativeSize() {
        assertThatThrownBy(() -> new CustomerPage(List.of(), 0L, 0, -5))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("size must be > 0");
    }
}
