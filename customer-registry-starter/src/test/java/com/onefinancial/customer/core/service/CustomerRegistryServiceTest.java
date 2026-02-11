package com.onefinancial.customer.core.service;

import com.onefinancial.customer.core.event.CustomerCreated;
import com.onefinancial.customer.core.event.CustomerDeleted;
import com.onefinancial.customer.core.event.CustomerStatusChanged;
import com.onefinancial.customer.core.event.CustomerUpdated;
import com.onefinancial.customer.core.exception.CustomerNotFoundException;
import com.onefinancial.customer.core.exception.CustomerValidationException;
import com.onefinancial.customer.core.exception.DuplicateDocumentException;
import com.onefinancial.customer.core.exception.InvalidStatusTransitionException;
import com.onefinancial.customer.core.model.AttributeValue;
import com.onefinancial.customer.core.model.Attributes;
import com.onefinancial.customer.core.model.Customer;
import com.onefinancial.customer.core.model.CustomerStatus;
import com.onefinancial.customer.core.port.CustomerEventPublisher;
import com.onefinancial.customer.core.port.CustomerRepository;
import com.onefinancial.customer.core.spi.CustomerEnricher;
import com.onefinancial.customer.core.spi.CustomerValidator;
import com.onefinancial.customer.core.spi.ValidationResult;
import com.onefinancial.customer.core.spi.CustomerOperationMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerRegistryServiceTest {

    private static final String VALID_CPF = "52998224725";
    private static final String VALID_CNPJ = "11222333000181";

    @Mock
    private CustomerRepository repository;

    @Mock
    private CustomerEventPublisher eventPublisher;

    private CustomerRegistryService service;

    @BeforeEach
    void setUp() {
        service = new CustomerRegistryService(
            List.of(), List.of(), repository, eventPublisher);
    }

    @Nested
    class Registration {

        @Test
        void shouldRegisterNewCustomerAndPublishEvent() {
            Customer customer = Customer.createPF(VALID_CPF, "João Silva");
            when(repository.existsByDocument(any())).thenReturn(false);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Customer result = service.register(customer);

            assertThat(result).isNotNull();
            verify(repository).save(customer);

            ArgumentCaptor<CustomerCreated> captor = ArgumentCaptor.forClass(CustomerCreated.class);
            verify(eventPublisher).publish(captor.capture());
            assertThat(captor.getValue().customerId()).isEqualTo(customer.getId());
            assertThat(captor.getValue().customerType()).isEqualTo(customer.getType());
        }

        @Test
        void shouldRejectDuplicateDocument() {
            Customer customer = Customer.createPF(VALID_CPF, "João Silva");
            when(repository.existsByDocument(any())).thenReturn(true);

            assertThatThrownBy(() -> service.register(customer))
                .isInstanceOf(DuplicateDocumentException.class)
                .hasMessageContaining("already exists");

            verify(repository, never()).save(any());
            verify(eventPublisher, never()).publish(any(CustomerCreated.class));
        }
    }

    @Nested
    class ValidationPipeline {

        @Test
        void shouldRunAllValidatorsAndCollectErrors() {
            CustomerValidator v1 = c -> ValidationResult.invalid("error.name.too_short");
            CustomerValidator v2 = c -> ValidationResult.invalid("error.missing.email");
            CustomerValidator v3 = c -> ValidationResult.valid();

            service = new CustomerRegistryService(
                List.of(v1, v2, v3), List.of(), repository, eventPublisher);

            Customer customer = Customer.createPF(VALID_CPF, "Test");

            assertThatThrownBy(() -> service.register(customer))
                .isInstanceOf(CustomerValidationException.class)
                .satisfies(ex -> {
                    var errors = ((CustomerValidationException) ex).getErrorKeys();
                    assertThat(errors).containsExactly("error.name.too_short", "error.missing.email");
                });
        }

        @Test
        void shouldSkipPersistenceWhenValidationFails() {
            CustomerValidator failingValidator = c ->
                ValidationResult.invalid("error.custom");

            service = new CustomerRegistryService(
                List.of(failingValidator), List.of(), repository, eventPublisher);

            Customer customer = Customer.createPF(VALID_CPF, "Test");

            assertThatThrownBy(() -> service.register(customer))
                .isInstanceOf(CustomerValidationException.class);

            verify(repository, never()).save(any());
            verify(repository, never()).existsByDocument(any());
        }
    }

    @Nested
    class EnrichmentPipeline {

        @Test
        void shouldRunEnrichersAfterValidation() {
            CustomerEnricher enricher = customer -> {
                customer.setAttributes(
                    customer.getAttributes().with("enriched", new AttributeValue.BooleanValue(true)));
                return customer;
            };

            service = new CustomerRegistryService(
                List.of(), List.of(enricher), repository, eventPublisher);

            Customer customer = Customer.createPF(VALID_CPF, "Test");
            when(repository.existsByDocument(any())).thenReturn(false);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Customer result = service.register(customer);

            assertThat(result.getAttributes().containsKey("enriched")).isTrue();
        }

        @Test
        void shouldChainMultipleEnrichers() {
            CustomerEnricher enricher1 = customer -> {
                customer.setAttributes(
                    customer.getAttributes().with("step1", new AttributeValue.StringValue("done")));
                return customer;
            };
            CustomerEnricher enricher2 = customer -> {
                customer.setAttributes(
                    customer.getAttributes().with("step2", new AttributeValue.StringValue("done")));
                return customer;
            };

            service = new CustomerRegistryService(
                List.of(), List.of(enricher1, enricher2), repository, eventPublisher);

            Customer customer = Customer.createPF(VALID_CPF, "Test");
            when(repository.existsByDocument(any())).thenReturn(false);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Customer result = service.register(customer);

            assertThat(result.getAttributes().containsKey("step1")).isTrue();
            assertThat(result.getAttributes().containsKey("step2")).isTrue();
        }
    }

    @Nested
    class StatusChange {

        @Test
        void shouldChangeStatusAndPublishEvent() {
            Customer customer = Customer.createPF(VALID_CPF, "Test");
            when(repository.findById(customer.getId())).thenReturn(Optional.of(customer));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Customer result = service.changeStatus(customer.getId(), CustomerStatus.ACTIVE);

            assertThat(result.getStatus()).isEqualTo(CustomerStatus.ACTIVE);

            ArgumentCaptor<CustomerStatusChanged> captor =
                ArgumentCaptor.forClass(CustomerStatusChanged.class);
            verify(eventPublisher).publish(captor.capture());
            assertThat(captor.getValue().fromStatus()).isEqualTo(CustomerStatus.DRAFT);
            assertThat(captor.getValue().toStatus()).isEqualTo(CustomerStatus.ACTIVE);
        }

        @Test
        void shouldRejectInvalidTransition() {
            Customer customer = Customer.createPF(VALID_CPF, "Test");
            when(repository.findById(customer.getId())).thenReturn(Optional.of(customer));

            assertThatThrownBy(() ->
                service.changeStatus(customer.getId(), CustomerStatus.SUSPENDED))
                .isInstanceOf(InvalidStatusTransitionException.class);

            verify(repository, never()).save(any());
        }

        @Test
        void shouldThrowWhenCustomerNotFound() {
            var unknownId = java.util.UUID.randomUUID();
            when(repository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                service.changeStatus(unknownId, CustomerStatus.ACTIVE))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining("not found");
        }
    }

    @Nested
    class Update {

        @Test
        void shouldUpdateCustomerAndPublishEvent() {
            Customer customer = Customer.createPF(VALID_CPF, "Updated Name");
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Customer result = service.update(customer);

            verify(repository).save(customer);
            verify(eventPublisher).publish(any(CustomerUpdated.class));
        }
    }

    @Nested
    class Deletion {

        @Test
        void shouldDeleteCustomerAndPublishEvent() {
            Customer customer = Customer.createPF(VALID_CPF, "Test");
            when(repository.findById(customer.getId())).thenReturn(Optional.of(customer));

            service.deleteCustomer(customer.getId());

            verify(repository).deleteById(customer.getId());
            ArgumentCaptor<CustomerDeleted> captor = ArgumentCaptor.forClass(CustomerDeleted.class);
            verify(eventPublisher).publish(captor.capture());
            assertThat(captor.getValue().customerId()).isEqualTo(customer.getId());
        }

        @Test
        void shouldThrowWhenDeletingNonExistentCustomer() {
            var unknownId = java.util.UUID.randomUUID();
            when(repository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteCustomer(unknownId))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining("not found");

            verify(repository, never()).deleteById(any());
            verify(eventPublisher, never()).publish(any(CustomerDeleted.class));
        }
    }

    @Nested
    class Queries {

        @Test
        void shouldDelegateToRepository() {
            var customer = Customer.createPF(VALID_CPF, "Test");
            when(repository.findById(customer.getId())).thenReturn(Optional.of(customer));
            when(repository.findAll()).thenReturn(List.of(customer));

            assertThat(service.findById(customer.getId())).isPresent();
            assertThat(service.findAll()).hasSize(1);
        }

        @Test
        void shouldDelegateFindAllPaginatedToRepository() {
            var customer = Customer.createPF(VALID_CPF, "Test");
            var page = new com.onefinancial.customer.core.model.CustomerPage(List.of(customer), 1L, 0, 10);
            when(repository.findAll(0, 10)).thenReturn(page);

            var result = service.findAllPaginated(0, 10);

            assertThat(result.customers()).hasSize(1);
            assertThat(result.totalElements()).isEqualTo(1L);
            verify(repository).findAll(0, 10);
        }

        @Test
        void shouldDelegateFindByDocumentToRepository() {
            var customer = Customer.createPF(VALID_CPF, "Test");
            var document = customer.getDocument();
            when(repository.findByDocument(document)).thenReturn(Optional.of(customer));

            Optional<Customer> result = service.findByDocument(document);

            assertThat(result).isPresent();
            assertThat(result.get().getDocument().number()).isEqualTo(VALID_CPF);
            verify(repository).findByDocument(document);
        }

        @Test
        void shouldReturnEmptyWhenDocumentNotFound() {
            var document = new com.onefinancial.customer.core.model.Document(
                com.onefinancial.customer.core.model.CustomerType.PJ, VALID_CNPJ);
            when(repository.findByDocument(document)).thenReturn(Optional.empty());

            Optional<Customer> result = service.findByDocument(document);

            assertThat(result).isEmpty();
            verify(repository).findByDocument(document);
        }
    }

    @Nested
    @DisplayName("Metrics integration")
    class MetricsIntegration {

        @Mock
        private CustomerOperationMetrics metricsBean;

        private CustomerRegistryService serviceWithMetrics;

        @BeforeEach
        void setUp() {
            serviceWithMetrics = new CustomerRegistryService(
                List.of(), List.of(), repository, eventPublisher,
                Optional.of(metricsBean));
        }

        @Test
        @DisplayName("should record create operation metric on register")
        void shouldRecordMetricOnRegister() {
            Customer customer = Customer.createPF(VALID_CPF, "Test");
            when(repository.existsByDocument(any())).thenReturn(false);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            serviceWithMetrics.register(customer);

            verify(metricsBean).recordOperation(eq("create"), eq("success"), any());
        }

        @Test
        @DisplayName("should record update operation metric")
        void shouldRecordMetricOnUpdate() {
            Customer customer = Customer.createPF(VALID_CPF, "Test");
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            serviceWithMetrics.update(customer);

            verify(metricsBean).recordOperation(eq("update"), eq("success"), any());
        }

        @Test
        @DisplayName("should record error metric on failed register")
        void shouldRecordErrorMetricOnFailedRegister() {
            Customer customer = Customer.createPF(VALID_CPF, "Test");
            when(repository.existsByDocument(any())).thenReturn(true);

            assertThatThrownBy(() -> serviceWithMetrics.register(customer))
                .isInstanceOf(DuplicateDocumentException.class);

            verify(metricsBean).recordOperation(eq("create"), eq("error"), any());
        }

        @Test
        @DisplayName("should record delete operation metric")
        void shouldRecordMetricOnDelete() {
            Customer customer = Customer.createPF(VALID_CPF, "Test");
            when(repository.findById(customer.getId())).thenReturn(Optional.of(customer));

            serviceWithMetrics.deleteCustomer(customer.getId());

            verify(metricsBean).recordOperation(eq("delete"), eq("success"), any());
        }

        @Test
        @DisplayName("should record status_change operation metric")
        void shouldRecordMetricOnStatusChange() {
            Customer customer = Customer.createPF(VALID_CPF, "Test");
            when(repository.findById(customer.getId())).thenReturn(Optional.of(customer));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            serviceWithMetrics.changeStatus(customer.getId(), CustomerStatus.ACTIVE);

            verify(metricsBean).recordOperation(eq("status_change"), eq("success"), any());
        }

        @Test
        @DisplayName("should not fail when metrics are absent")
        void shouldNotFailWithoutMetrics() {
            // service without metrics (default constructor)
            Customer customer = Customer.createPF(VALID_CPF, "Test");
            when(repository.existsByDocument(any())).thenReturn(false);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.register(customer);

            // No exception — metrics are optional
        }
    }
}
