package com.oneff.customer.events;

import com.oneff.customer.core.event.CustomerCreated;
import com.oneff.customer.core.event.CustomerDeleted;
import com.oneff.customer.core.event.CustomerStatusChanged;
import com.oneff.customer.core.event.CustomerUpdated;
import com.oneff.customer.core.model.CustomerStatus;
import com.oneff.customer.core.model.CustomerType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.UUID;

import static org.mockito.Mockito.*;

class SpringEventPublisherAdapterTest {

    private ApplicationEventPublisher applicationEventPublisher;
    private SpringEventPublisherAdapter adapter;

    @BeforeEach
    void setUp() {
        applicationEventPublisher = mock(ApplicationEventPublisher.class);
        adapter = new SpringEventPublisherAdapter(applicationEventPublisher);
    }

    @Test
    @DisplayName("should publish CustomerCreated via ApplicationEventPublisher")
    void publishCustomerCreated() {
        CustomerCreated event = CustomerCreated.of(UUID.randomUUID(), CustomerType.PF);

        adapter.publish(event);

        verify(applicationEventPublisher).publishEvent(event);
    }

    @Test
    @DisplayName("should publish CustomerUpdated via ApplicationEventPublisher")
    void publishCustomerUpdated() {
        CustomerUpdated event = CustomerUpdated.of(UUID.randomUUID());

        adapter.publish(event);

        verify(applicationEventPublisher).publishEvent(event);
    }

    @Test
    @DisplayName("should publish CustomerStatusChanged via ApplicationEventPublisher")
    void publishCustomerStatusChanged() {
        CustomerStatusChanged event = CustomerStatusChanged.of(
            UUID.randomUUID(), CustomerStatus.DRAFT, CustomerStatus.ACTIVE);

        adapter.publish(event);

        verify(applicationEventPublisher).publishEvent(event);
    }

    @Test
    @DisplayName("should publish CustomerDeleted via ApplicationEventPublisher")
    void publishCustomerDeleted() {
        CustomerDeleted event = CustomerDeleted.of(UUID.randomUUID());

        adapter.publish(event);

        verify(applicationEventPublisher).publishEvent(event);
    }
}
