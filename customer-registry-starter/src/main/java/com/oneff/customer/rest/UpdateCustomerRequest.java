package com.oneff.customer.rest;

import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating an existing customer.
 * All fields are optional — only non-null fields are applied.
 *
 * <p>This record is {@code public} intentionally so that consumers of the library
 * can reference it in their own tests and request construction code.</p>
 *
 * @param displayName new display name (if provided)
 * @param status      new status to transition to (if provided)
 */
public record UpdateCustomerRequest(
    @Size(min = 1, max = 255, message = "Display name must be between 1 and 255 characters")
    String displayName,

    String status
) {}
