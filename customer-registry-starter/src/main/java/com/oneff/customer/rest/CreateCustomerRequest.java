package com.oneff.customer.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for creating a new customer.
 *
 * @param type        customer type: "PF" or "PJ"
 * @param document    CPF (11 digits) or CNPJ (14 digits), unformatted
 * @param displayName the customer's display name
 */
public record CreateCustomerRequest(
    @NotNull @Pattern(regexp = "PF|PJ", message = "Type must be PF or PJ")
    String type,

    @NotBlank(message = "Document number is required")
    String document,

    @NotBlank(message = "Display name is required")
    String displayName
) {}
