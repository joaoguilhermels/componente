--liquibase formatted sql

--changeset customer-registry:v1.0.1-001-remove-redundant-index
--comment The UNIQUE constraint on document_number already creates an implicit index, making idx_cr_customer_document redundant.
DROP INDEX IF EXISTS idx_cr_customer_document;
--rollback CREATE INDEX IF NOT EXISTS idx_cr_customer_document ON cr_customer(document_number);
