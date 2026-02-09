package com.onefinancial.customer.core.exception;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerNotFoundExceptionTest {

    @Test
    void shouldStoreCustomerIdForProgrammaticAccess() {
        var id = UUID.randomUUID();
        var ex = new CustomerNotFoundException(id);

        assertThat(ex.getCustomerId()).isEqualTo(id);
        assertThat(ex.getMessage()).contains(id.toString());
    }

    @Test
    void shouldRejectNullCustomerId() {
        assertThatThrownBy(() -> new CustomerNotFoundException(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("customerId must not be null");
    }
}
