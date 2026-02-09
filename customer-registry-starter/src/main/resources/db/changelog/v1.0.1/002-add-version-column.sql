--liquibase formatted sql

--changeset customer-registry:v1.0.1-002-add-version-column
--comment Adds optimistic locking version column for JPA @Version support.
ALTER TABLE cr_customer ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
--rollback ALTER TABLE cr_customer DROP COLUMN IF EXISTS version;
