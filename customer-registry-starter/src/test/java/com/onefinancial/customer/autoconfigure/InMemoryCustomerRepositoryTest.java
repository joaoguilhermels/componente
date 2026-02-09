package com.onefinancial.customer.autoconfigure;

import com.onefinancial.customer.core.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryCustomerRepositoryTest {

    private static final String VALID_CPF = "52998224725";
    private static final String VALID_CNPJ = "11222333000181";

    private InMemoryCustomerRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryCustomerRepository();
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("should save and return customer")
        void savesAndReturnsCustomer() {
            Customer customer = Customer.createPF(VALID_CPF, "Maria Silva");

            Customer saved = repository.save(customer);

            assertThat(saved).isSameAs(customer);
        }

        @Test
        @DisplayName("should overwrite existing customer with same ID (update)")
        void overwritesExistingCustomer() {
            Customer customer = Customer.createPF(VALID_CPF, "Maria Silva");
            repository.save(customer);

            customer.updateDisplayName("Maria Santos");
            repository.save(customer);

            Optional<Customer> found = repository.findById(customer.getId());
            assertThat(found).isPresent();
            assertThat(found.get().getDisplayName()).isEqualTo("Maria Santos");
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return customer when exists")
        void returnsCustomerWhenExists() {
            Customer customer = Customer.createPF(VALID_CPF, "Maria Silva");
            repository.save(customer);

            Optional<Customer> found = repository.findById(customer.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(customer.getId());
        }

        @Test
        @DisplayName("should return empty when not found")
        void returnsEmptyWhenNotFound() {
            Optional<Customer> found = repository.findById(UUID.randomUUID());

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByDocument")
    class FindByDocument {

        @Test
        @DisplayName("should return customer when document matches")
        void returnsCustomerWhenDocumentMatches() {
            Customer customer = Customer.createPF(VALID_CPF, "Maria Silva");
            repository.save(customer);

            Document document = new Document(CustomerType.PF, VALID_CPF);
            Optional<Customer> found = repository.findByDocument(document);

            assertThat(found).isPresent();
            assertThat(found.get().getDocument().number()).isEqualTo(VALID_CPF);
        }

        @Test
        @DisplayName("should return empty when document not found")
        void returnsEmptyWhenDocumentNotFound() {
            Document document = new Document(CustomerType.PJ, VALID_CNPJ);
            Optional<Customer> found = repository.findByDocument(document);

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByDocument")
    class ExistsByDocument {

        @Test
        @DisplayName("should return true when document exists")
        void returnsTrueWhenExists() {
            Customer customer = Customer.createPF(VALID_CPF, "Maria Silva");
            repository.save(customer);

            Document document = new Document(CustomerType.PF, VALID_CPF);

            assertThat(repository.existsByDocument(document)).isTrue();
        }

        @Test
        @DisplayName("should return false when document does not exist")
        void returnsFalseWhenNotExists() {
            Document document = new Document(CustomerType.PJ, VALID_CNPJ);

            assertThat(repository.existsByDocument(document)).isFalse();
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("should return all saved customers")
        void returnsAllCustomers() {
            Customer c1 = Customer.createPF(VALID_CPF, "Maria Silva");
            Customer c2 = Customer.createPJ(VALID_CNPJ, "Empresa LTDA");
            repository.save(c1);
            repository.save(c2);

            List<Customer> all = repository.findAll();

            assertThat(all).hasSize(2);
        }

        @Test
        @DisplayName("should return empty list when no customers")
        void returnsEmptyListWhenEmpty() {
            List<Customer> all = repository.findAll();

            assertThat(all).isEmpty();
        }

        @Test
        @DisplayName("should return a defensive copy")
        void returnsDefensiveCopy() {
            Customer customer = Customer.createPF(VALID_CPF, "Maria Silva");
            repository.save(customer);

            List<Customer> all = repository.findAll();
            all.clear();

            assertThat(repository.findAll()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("findAll paginated")
    class FindAllPaginated {

        @Test
        @DisplayName("should return first page of customers")
        void returnsFirstPage() {
            for (int i = 0; i < 5; i++) {
                Customer c = Customer.createPF(VALID_CPF, "Customer-" + i);
                repository.save(c);
            }

            CustomerPage page = repository.findAll(0, 2);

            assertThat(page.customers()).hasSize(2);
            assertThat(page.totalElements()).isEqualTo(5);
            assertThat(page.page()).isEqualTo(0);
            assertThat(page.size()).isEqualTo(2);
        }

        @Test
        @DisplayName("should return empty page beyond data range")
        void returnsEmptyPageBeyondRange() {
            Customer c = Customer.createPF(VALID_CPF, "Test");
            repository.save(c);

            CustomerPage page = repository.findAll(10, 20);

            assertThat(page.customers()).isEmpty();
            assertThat(page.totalElements()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("deleteById")
    class DeleteById {

        @Test
        @DisplayName("should remove customer by ID")
        void removesCustomer() {
            Customer customer = Customer.createPF(VALID_CPF, "Test");
            repository.save(customer);

            repository.deleteById(customer.getId());

            assertThat(repository.findById(customer.getId())).isEmpty();
            assertThat(repository.findAll()).isEmpty();
        }

        @Test
        @DisplayName("should be no-op when ID does not exist")
        void noOpWhenNotExists() {
            repository.deleteById(UUID.randomUUID());

            assertThat(repository.findAll()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Thread safety")
    class ThreadSafety {

        @Test
        @DisplayName("should handle concurrent save operations without errors")
        void concurrentSaves() throws InterruptedException {
            int threadCount = 10;
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            try {
                for (int i = 0; i < threadCount; i++) {
                    final int index = i;
                    executor.submit(() -> {
                        try {
                            // Each thread saves a unique customer
                            Customer customer = Customer.createPF(VALID_CPF, "Thread-" + index);
                            repository.save(customer);
                            successCount.incrementAndGet();
                        } finally {
                            latch.countDown();
                        }
                    });
                }

                boolean completed = latch.await(5, TimeUnit.SECONDS);
                assertThat(completed).isTrue();
                assertThat(successCount.get()).isEqualTo(threadCount);
                assertThat(repository.findAll().size()).isEqualTo(threadCount);
            } finally {
                executor.shutdownNow();
            }
        }
    }
}
