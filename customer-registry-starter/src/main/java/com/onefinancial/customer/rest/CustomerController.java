package com.onefinancial.customer.rest;

import com.onefinancial.customer.core.model.Customer;
import com.onefinancial.customer.core.model.CustomerPage;
import com.onefinancial.customer.core.model.CustomerStatus;
import com.onefinancial.customer.core.model.CustomerType;
import com.onefinancial.customer.core.model.Document;
import com.onefinancial.customer.core.service.CustomerRegistryService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * REST controller exposing the Customer Registry CRUD operations.
 *
 * <p>All endpoints are prefixed with {@code /api/v1/customers}.
 * This controller is intentionally thin — it maps HTTP concerns to domain
 * operations and delegates all business logic to {@link CustomerRegistryService}.</p>
 */
@RestController
@RequestMapping("/api/v1/customers")
class CustomerController {

    private final CustomerRegistryService service;

    CustomerController(CustomerRegistryService service) {
        this.service = service;
    }

    @PostMapping
    ResponseEntity<CustomerResponse> create(@Valid @RequestBody CreateCustomerRequest request) {
        CustomerType type = CustomerType.valueOf(request.type());
        Customer customer = type == CustomerType.PF
            ? Customer.createPF(request.document(), request.displayName())
            : Customer.createPJ(request.document(), request.displayName());

        Customer saved = service.register(customer);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(saved.getId())
            .toUri();

        return ResponseEntity.created(location).body(CustomerResponse.from(saved));
    }

    @GetMapping("/{id}")
    ResponseEntity<CustomerResponse> findById(@PathVariable UUID id) {
        return service.findById(id)
            .map(CustomerResponse::from)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-document/{document}")
    ResponseEntity<CustomerResponse> findByDocument(@PathVariable String document) {
        // Try PF first (11 digits), then PJ (14 digits)
        CustomerType type = document.replaceAll("[.\\-/]", "").length() <= 11
            ? CustomerType.PF : CustomerType.PJ;
        Document doc = new Document(type, document);

        return service.findByDocument(doc)
            .map(CustomerResponse::from)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    ResponseEntity<CustomerPageResponse> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        CustomerPage customerPage = service.findAllPaginated(page, size);

        List<CustomerResponse> responses = customerPage.customers().stream()
            .map(CustomerResponse::fromMasked)
            .toList();

        int totalPages = size > 0
            ? (int) Math.ceil((double) customerPage.totalElements() / size)
            : 0;

        CustomerPageResponse response = new CustomerPageResponse(
            responses,
            customerPage.totalElements(),
            customerPage.page(),
            customerPage.size(),
            totalPages
        );

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    ResponseEntity<CustomerResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCustomerRequest request) {

        return service.findById(id)
            .map(customer -> {
                Customer result = customer;

                // Apply display name change first if provided
                if (request.displayName() != null) {
                    result.updateDisplayName(request.displayName());
                    result = service.update(result);
                }

                // Apply status change second if provided
                if (request.status() != null) {
                    CustomerStatus newStatus = CustomerStatus.valueOf(request.status());
                    result = service.changeStatus(result.getId(), newStatus);
                }

                return result;
            })
            .map(CustomerResponse::from)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }
}
