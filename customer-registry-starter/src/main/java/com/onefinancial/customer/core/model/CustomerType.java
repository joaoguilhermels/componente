package com.onefinancial.customer.core.model;

/**
 * Classification of a customer entity in the Brazilian regulatory context.
 *
 * <ul>
 *   <li>{@code PF} — Pessoa Física (individual, CPF document)</li>
 *   <li>{@code PJ} — Pessoa Jurídica (legal entity, CNPJ document)</li>
 * </ul>
 */
public enum CustomerType {
    PF,
    PJ
}
