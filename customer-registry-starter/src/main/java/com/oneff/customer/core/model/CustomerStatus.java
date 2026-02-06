package com.oneff.customer.core.model;

import java.util.Set;

/**
 * Lifecycle status of a customer record.
 *
 * <p>Allowed transitions:
 * <pre>
 * DRAFT → ACTIVE → SUSPENDED → CLOSED
 *                → CLOSED
 *       → CLOSED
 * </pre>
 */
public enum CustomerStatus {

    DRAFT(Set.of()),
    ACTIVE(Set.of()),
    SUSPENDED(Set.of()),
    CLOSED(Set.of());

    private Set<CustomerStatus> allowedTransitions;

    CustomerStatus(Set<CustomerStatus> allowedTransitions) {
        this.allowedTransitions = allowedTransitions;
    }

    static {
        DRAFT.allowedTransitions = Set.of(ACTIVE, CLOSED);
        ACTIVE.allowedTransitions = Set.of(SUSPENDED, CLOSED);
        SUSPENDED.allowedTransitions = Set.of(ACTIVE, CLOSED);
        CLOSED.allowedTransitions = Set.of();
    }

    public boolean canTransitionTo(CustomerStatus target) {
        return allowedTransitions.contains(target);
    }
}
