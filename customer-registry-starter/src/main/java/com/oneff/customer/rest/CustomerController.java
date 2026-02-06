package com.oneff.customer.rest;

import com.oneff.customer.core.model.Customer;
import com.oneff.customer.core.model.CustomerStatus;
import com.oneff.customer.core.model.CustomerType;
import com.oneff.customer.core.model.Document;
import com.oneff.customer.core.service.CustomerRegistryService;

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
    List<CustomerResponse> findAll() {
        return service.findAll().stream()
            .map(CustomerResponse::from)
            .toList();
    }

    @PatchMapping("/{id}")
    ResponseEntity<CustomerResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCustomerRequest request) {

        return service.findById(id)
            .map(customer -> {
                if (request.displayName() != null) {
                    customer.updateDisplayName(request.displayName());
                }
                if (request.status() != null) {
                    CustomerStatus newStatus = CustomerStatus.valueOf(request.status());
                    return service.changeStatus(customer.getId(), newStatus);
                }
                return service.update(customer);
            })
            .map(CustomerResponse::from)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
