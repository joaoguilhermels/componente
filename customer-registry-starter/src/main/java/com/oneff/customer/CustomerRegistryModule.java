package com.oneff.customer;

import org.springframework.modulith.Modulithic;

/**
 * Marker class for the Customer Registry module root.
 * Used by Spring Modulith to discover module boundaries.
 *
 * <p>Annotated with {@link Modulithic} rather than {@code @SpringBootApplication}
 * because this is a reusable library, not a standalone application.</p>
 */
@Modulithic
public final class CustomerRegistryModule {

    private CustomerRegistryModule() {
        // Marker class — not instantiable
    }
}
