package com.onefinancial.customer.core.event;

import com.onefinancial.customer.core.model.CustomerStatus;
import com.onefinancial.customer.core.model.CustomerType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerEventValidationTest {

    private static final UUID ID = UUID.randomUUID();
    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.now();

    @Nested
    class CustomerCreatedValidation {

        @Test
        void shouldCreateViaFactory() {
            var event = CustomerCreated.of(ID, CustomerType.PF);

            assertThat(event.customerId()).isEqualTo(ID);
            assertThat(event.customerType()).isEqualTo(CustomerType.PF);
            assertThat(event.eventId()).isNotNull();
            assertThat(event.occurredAt()).isNotNull();
        }

        @Test
        void shouldRejectNullEventId() {
            assertThatThrownBy(() -> new CustomerCreated(null, ID, CustomerType.PF, NOW))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("eventId");
        }

        @Test
        void shouldRejectNullCustomerId() {
            assertThatThrownBy(() -> new CustomerCreated(EVENT_ID, null, CustomerType.PF, NOW))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("customerId");
        }

        @Test
        void shouldRejectNullCustomerType() {
            assertThatThrownBy(() -> new CustomerCreated(EVENT_ID, ID, null, NOW))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("customerType");
        }

        @Test
        void shouldRejectNullOccurredAt() {
            assertThatThrownBy(() -> new CustomerCreated(EVENT_ID, ID, CustomerType.PF, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("occurredAt");
        }

        @Test
        void shouldGenerateUniqueEventIdForSuccessiveCalls() {
            var event1 = CustomerCreated.of(ID, CustomerType.PF);
            var event2 = CustomerCreated.of(ID, CustomerType.PF);

            assertThat(event1.eventId()).isNotEqualTo(event2.eventId());
        }

        @Test
        void shouldGenerateDifferentEventIdForDifferentCustomers() {
            var otherId = UUID.randomUUID();
            var event1 = CustomerCreated.of(ID, CustomerType.PF);
            var event2 = CustomerCreated.of(otherId, CustomerType.PF);

            assertThat(event1.eventId()).isNotEqualTo(event2.eventId());
        }
    }

    @Nested
    class CustomerDeletedValidation {

        @Test
        void shouldCreateViaFactory() {
            var event = CustomerDeleted.of(ID);

            assertThat(event.customerId()).isEqualTo(ID);
            assertThat(event.eventId()).isNotNull();
            assertThat(event.occurredAt()).isNotNull();
        }

        @Test
        void shouldRejectNullEventId() {
            assertThatThrownBy(() -> new CustomerDeleted(null, ID, NOW))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("eventId");
        }

        @Test
        void shouldRejectNullCustomerId() {
            assertThatThrownBy(() -> new CustomerDeleted(EVENT_ID, null, NOW))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("customerId");
        }

        @Test
        void shouldRejectNullOccurredAt() {
            assertThatThrownBy(() -> new CustomerDeleted(EVENT_ID, ID, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("occurredAt");
        }

        @Test
        void shouldGenerateUniqueEventIdForSuccessiveCalls() {
            var event1 = CustomerDeleted.of(ID);
            var event2 = CustomerDeleted.of(ID);

            assertThat(event1.eventId()).isNotEqualTo(event2.eventId());
        }
    }

    @Nested
    class CustomerUpdatedValidation {

        @Test
        void shouldCreateViaFactory() {
            var event = CustomerUpdated.of(ID);

            assertThat(event.customerId()).isEqualTo(ID);
            assertThat(event.eventId()).isNotNull();
            assertThat(event.occurredAt()).isNotNull();
        }

        @Test
        void shouldRejectNullEventId() {
            assertThatThrownBy(() -> new CustomerUpdated(null, ID, NOW))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("eventId");
        }

        @Test
        void shouldRejectNullCustomerId() {
            assertThatThrownBy(() -> new CustomerUpdated(EVENT_ID, null, NOW))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("customerId");
        }

        @Test
        void shouldRejectNullOccurredAt() {
            assertThatThrownBy(() -> new CustomerUpdated(EVENT_ID, ID, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("occurredAt");
        }

        @Test
        void shouldGenerateUniqueEventIdForSuccessiveCalls() {
            var event1 = CustomerUpdated.of(ID);
            var event2 = CustomerUpdated.of(ID);

            assertThat(event1.eventId()).isNotEqualTo(event2.eventId());
        }
    }

    @Nested
    class CustomerStatusChangedValidation {

        @Test
        void shouldCreateViaFactory() {
            var event = CustomerStatusChanged.of(ID, CustomerStatus.DRAFT, CustomerStatus.ACTIVE);

            assertThat(event.customerId()).isEqualTo(ID);
            assertThat(event.fromStatus()).isEqualTo(CustomerStatus.DRAFT);
            assertThat(event.toStatus()).isEqualTo(CustomerStatus.ACTIVE);
            assertThat(event.eventId()).isNotNull();
            assertThat(event.occurredAt()).isNotNull();
        }

        @Test
        void shouldRejectNullEventId() {
            assertThatThrownBy(() -> new CustomerStatusChanged(null, ID, CustomerStatus.DRAFT, CustomerStatus.ACTIVE, NOW))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("eventId");
        }

        @Test
        void shouldRejectNullCustomerId() {
            assertThatThrownBy(() -> new CustomerStatusChanged(EVENT_ID, null, CustomerStatus.DRAFT, CustomerStatus.ACTIVE, NOW))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("customerId");
        }

        @Test
        void shouldRejectNullFromStatus() {
            assertThatThrownBy(() -> new CustomerStatusChanged(EVENT_ID, ID, null, CustomerStatus.ACTIVE, NOW))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("fromStatus");
        }

        @Test
        void shouldRejectNullToStatus() {
            assertThatThrownBy(() -> new CustomerStatusChanged(EVENT_ID, ID, CustomerStatus.DRAFT, null, NOW))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("toStatus");
        }

        @Test
        void shouldRejectNullOccurredAt() {
            assertThatThrownBy(() -> new CustomerStatusChanged(EVENT_ID, ID, CustomerStatus.DRAFT, CustomerStatus.ACTIVE, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("occurredAt");
        }

        @Test
        void shouldGenerateUniqueEventIdForSuccessiveCalls() {
            var event1 = CustomerStatusChanged.of(ID, CustomerStatus.DRAFT, CustomerStatus.ACTIVE);
            var event2 = CustomerStatusChanged.of(ID, CustomerStatus.DRAFT, CustomerStatus.ACTIVE);

            assertThat(event1.eventId()).isNotEqualTo(event2.eventId());
        }
    }
}
