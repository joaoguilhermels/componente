--liquibase formatted sql

--changeset customer-registry:v1.0.1-003-add-contact-composite-index
--comment Composite index on cr_contact(customer_id, is_primary) to optimize primary contact lookups per customer.
CREATE INDEX IF NOT EXISTS idx_cr_contact_customer_primary ON cr_contact(customer_id, is_primary);
--rollback DROP INDEX IF EXISTS idx_cr_contact_customer_primary;
