package com.onefinancial.customer.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new customer.
 *
 * <p>This record is {@code public} intentionally so that consumers of the library
 * can reference it in their own tests and request construction code.</p>
 *
 * @param type        customer type: "PF" or "PJ"
 * @param document    CPF (11 digits) or CNPJ (14 digits), unformatted
 * @param displayName the customer's display name
 */
public record CreateCustomerRequest(
    @NotNull @Pattern(regexp = "PF|PJ", message = "Type must be PF or PJ")
    String type,

    @NotBlank(message = "Document number is required")
    @Size(max = 20, message = "Document must not exceed 20 characters")
    String document,

    @NotBlank(message = "Display name is required")
    @Size(min = 1, max = 255, message = "Display name must be between 1 and 255 characters")
    String displayName
) {}
