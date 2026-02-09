package com.oneff.customer.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oneff.customer.core.exception.CustomerValidationException;
import com.oneff.customer.core.exception.DocumentValidationException;
import com.oneff.customer.core.exception.DuplicateDocumentException;
import com.oneff.customer.core.exception.InvalidStatusTransitionException;
import com.oneff.customer.core.model.*;
import static org.mockito.ArgumentMatchers.anyInt;
import com.oneff.customer.core.service.CustomerRegistryService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({CustomerController.class, CustomerExceptionHandler.class})
class CustomerControllerTest {

    /**
     * Minimal Spring Boot application for {@code @WebMvcTest} bootstrap.
     * Excludes auto-configurations that aren't needed for controller testing.
     */
    @SpringBootApplication(
        scanBasePackages = "com.oneff.customer.rest",
        exclude = {
            org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
            org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class
        }
    )
    static class TestApp {
        @Bean("customerRegistryMessageSource")
        MessageSource customerRegistryMessageSource() {
            ReloadableResourceBundleMessageSource source =
                new ReloadableResourceBundleMessageSource();
            source.setBasename("classpath:messages/customer-registry-messages");
            source.setDefaultEncoding("UTF-8");
            return source;
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CustomerRegistryService service;

    private static final String VALID_CPF = "52998224725";
    private static final String VALID_CNPJ = "11222333000181";

    private Customer samplePfCustomer() {
        return Customer.createPF(VALID_CPF, "Maria Silva");
    }

    // ─── POST /api/v1/customers ─────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/customers")
    class CreateCustomer {

        @Test
        @DisplayName("should create PF customer and return 201 with Location header")
        void createPfCustomer() throws Exception {
            Customer customer = samplePfCustomer();
            when(service.register(any(Customer.class))).thenReturn(customer);

            mockMvc.perform(post("/api/v1/customers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"type":"PF","document":"52998224725","displayName":"Maria Silva"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                    containsString("/api/v1/customers/" + customer.getId())))
                .andExpect(jsonPath("$.id").value(customer.getId().toString()))
                .andExpect(jsonPath("$.type").value("PF"))
                .andExpect(jsonPath("$.displayName").value("Maria Silva"));
        }

        @Test
        @DisplayName("should create PJ customer")
        void createPjCustomer() throws Exception {
            Customer customer = Customer.createPJ(VALID_CNPJ, "Acme Ltda");
            when(service.register(any(Customer.class))).thenReturn(customer);

            mockMvc.perform(post("/api/v1/customers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"type":"PJ","document":"11222333000181","displayName":"Acme Ltda"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("PJ"));
        }

        @Test
        @DisplayName("should return 400 for missing required fields")
        void rejectMissingFields() throws Exception {
            mockMvc.perform(post("/api/v1/customers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.errors").isArray());
        }

        @Test
        @DisplayName("should return 400 for invalid type")
        void rejectInvalidType() throws Exception {
            mockMvc.perform(post("/api/v1/customers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"type":"XX","document":"52998224725","displayName":"Test"}
                        """))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 409 for duplicate document")
        void rejectDuplicateDocument() throws Exception {
            when(service.register(any(Customer.class)))
                .thenThrow(new DuplicateDocumentException());

            mockMvc.perform(post("/api/v1/customers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"type":"PF","document":"52998224725","displayName":"Maria Silva"}
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate Document"));
        }

        @Test
        @DisplayName("should return 400 for invalid document")
        void rejectInvalidDocument() throws Exception {
            when(service.register(any(Customer.class)))
                .thenThrow(new DocumentValidationException("CPF checksum failed"));

            mockMvc.perform(post("/api/v1/customers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"type":"PF","document":"52998224725","displayName":"Test"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Document Validation Error"));
        }

        @Test
        @DisplayName("should return 400 with errors array for CustomerValidationException")
        void rejectWithValidationErrors() throws Exception {
            when(service.register(any(Customer.class)))
                .thenThrow(new CustomerValidationException(
                    java.util.List.of("error.name.too_short", "error.missing.email")));

            mockMvc.perform(post("/api/v1/customers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"type":"PF","document":"52998224725","displayName":"Test"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors", hasSize(2)));
        }
    }

    // ─── GET /api/v1/customers/{id} ─────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/customers/{id}")
    class FindById {

        @Test
        @DisplayName("should return customer when found")
        void returnCustomerWhenFound() throws Exception {
            Customer customer = samplePfCustomer();
            when(service.findById(customer.getId())).thenReturn(Optional.of(customer));

            mockMvc.perform(get("/api/v1/customers/{id}", customer.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customer.getId().toString()))
                .andExpect(jsonPath("$.displayName").value("Maria Silva"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
        }

        @Test
        @DisplayName("should return 404 when not found")
        void return404WhenNotFound() throws Exception {
            UUID id = UUID.randomUUID();
            when(service.findById(id)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/customers/{id}", id))
                .andExpect(status().isNotFound());
        }
    }

    // ─── GET /api/v1/customers/by-document/{doc} ─────────────────

    @Nested
    @DisplayName("GET /api/v1/customers/by-document/{doc}")
    class FindByDocument {

        @Test
        @DisplayName("should return customer by CPF document")
        void findByCpf() throws Exception {
            Customer customer = samplePfCustomer();
            when(service.findByDocument(any(Document.class)))
                .thenReturn(Optional.of(customer));

            mockMvc.perform(get("/api/v1/customers/by-document/{doc}", VALID_CPF))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.document").exists());
        }

        @Test
        @DisplayName("should return 404 when document not found")
        void return404WhenDocNotFound() throws Exception {
            when(service.findByDocument(any(Document.class)))
                .thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/customers/by-document/{doc}", VALID_CPF))
                .andExpect(status().isNotFound());
        }
    }

    // ─── GET /api/v1/customers ───────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/customers")
    class FindAll {

        @Test
        @DisplayName("should return paginated list of customers")
        void returnCustomerList() throws Exception {
            Customer c1 = Customer.createPF(VALID_CPF, "Maria Silva");
            Customer c2 = Customer.createPJ(VALID_CNPJ, "Acme Ltda");
            when(service.findAllPaginated(anyInt(), anyInt()))
                .thenReturn(new CustomerPage(List.of(c1, c2), 2, 0, 20));

            mockMvc.perform(get("/api/v1/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customers", hasSize(2)))
                .andExpect(jsonPath("$.customers[0].type").value("PF"))
                .andExpect(jsonPath("$.customers[1].type").value("PJ"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
        }

        @Test
        @DisplayName("should return empty page when no customers")
        void returnEmptyList() throws Exception {
            when(service.findAllPaginated(anyInt(), anyInt()))
                .thenReturn(new CustomerPage(List.of(), 0, 0, 20));

            mockMvc.perform(get("/api/v1/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customers", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
        }
    }

    // ─── PATCH /api/v1/customers/{id} ─────────────────────────────

    @Nested
    @DisplayName("PATCH /api/v1/customers/{id}")
    class UpdateCustomer {

        @Test
        @DisplayName("should update display name")
        void updateDisplayName() throws Exception {
            Customer customer = samplePfCustomer();
            when(service.findById(customer.getId())).thenReturn(Optional.of(customer));
            when(service.update(any(Customer.class))).thenReturn(customer);

            mockMvc.perform(patch("/api/v1/customers/{id}", customer.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"displayName":"Maria Santos"}
                        """))
                .andExpect(status().isOk());

            verify(service).update(any(Customer.class));
        }

        @Test
        @DisplayName("should change status")
        void changeStatus() throws Exception {
            Customer customer = samplePfCustomer();
            when(service.findById(customer.getId())).thenReturn(Optional.of(customer));
            when(service.changeStatus(customer.getId(), CustomerStatus.ACTIVE))
                .thenReturn(customer);

            mockMvc.perform(patch("/api/v1/customers/{id}", customer.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"status":"ACTIVE"}
                        """))
                .andExpect(status().isOk());

            verify(service).changeStatus(customer.getId(), CustomerStatus.ACTIVE);
        }

        @Test
        @DisplayName("should return 404 when customer not found for update")
        void return404ForUpdate() throws Exception {
            UUID id = UUID.randomUUID();
            when(service.findById(id)).thenReturn(Optional.empty());

            mockMvc.perform(patch("/api/v1/customers/{id}", id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"displayName":"New Name"}
                        """))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 422 for invalid status transition")
        void rejectInvalidTransition() throws Exception {
            Customer customer = samplePfCustomer();
            when(service.findById(customer.getId())).thenReturn(Optional.of(customer));
            when(service.changeStatus(customer.getId(), CustomerStatus.SUSPENDED))
                .thenThrow(new InvalidStatusTransitionException(
                    CustomerStatus.DRAFT, CustomerStatus.SUSPENDED));

            mockMvc.perform(patch("/api/v1/customers/{id}", customer.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"status":"SUSPENDED"}
                        """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Invalid Status Transition"));
        }

        @Test
        @DisplayName("should apply both displayName and status in single PATCH")
        void updateDisplayNameAndStatus() throws Exception {
            Customer customer = samplePfCustomer();
            when(service.findById(customer.getId())).thenReturn(Optional.of(customer));
            when(service.update(any(Customer.class))).thenReturn(customer);
            when(service.changeStatus(customer.getId(), CustomerStatus.ACTIVE))
                .thenReturn(customer);

            mockMvc.perform(patch("/api/v1/customers/{id}", customer.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"displayName":"Maria Santos","status":"ACTIVE"}
                        """))
                .andExpect(status().isOk());

            // Verify both operations were called
            verify(service).update(any(Customer.class));
            verify(service).changeStatus(customer.getId(), CustomerStatus.ACTIVE);
        }
    }

    // ─── i18n ───────────────────────────────────────────────────

    @Nested
    @DisplayName("i18n error messages")
    class I18nErrors {

        @Test
        @DisplayName("should return localized error for pt-BR")
        void localizedPtBr() throws Exception {
            when(service.register(any(Customer.class)))
                .thenThrow(new DuplicateDocumentException());

            mockMvc.perform(post("/api/v1/customers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Accept-Language", "pt-BR")
                    .content("""
                        {"type":"PF","document":"52998224725","displayName":"Maria Silva"}
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").isString());
        }
    }
}
