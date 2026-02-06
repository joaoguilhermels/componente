--liquibase formatted sql

--changeset customer-registry:001-create-customer-table
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'cr_customer'
CREATE TABLE IF NOT EXISTS cr_customer (
  id              UUID PRIMARY KEY,
  type            VARCHAR(2)   NOT NULL,
  document_number VARCHAR(14)  NOT NULL,
  display_name    VARCHAR(255) NOT NULL,
  status          VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
  attributes      JSONB        DEFAULT '{"schemaVersion":1,"values":{}}',
  schema_version  INTEGER      NOT NULL DEFAULT 1,
  created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
  updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
  CONSTRAINT uq_cr_customer_document UNIQUE (document_number)
);
--rollback DROP TABLE IF EXISTS cr_customer;

--changeset customer-registry:001-create-address-table
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'cr_address'
CREATE TABLE IF NOT EXISTS cr_address (
  id             UUID PRIMARY KEY,
  customer_id    UUID         NOT NULL REFERENCES cr_customer(id) ON DELETE CASCADE,
  street         VARCHAR(255),
  address_number VARCHAR(20),
  complement     VARCHAR(100),
  neighborhood   VARCHAR(100),
  city           VARCHAR(100) NOT NULL,
  state          VARCHAR(2)   NOT NULL,
  zip_code       VARCHAR(10)  NOT NULL,
  country        VARCHAR(2)   NOT NULL DEFAULT 'BR'
);
--rollback DROP TABLE IF EXISTS cr_address;

--changeset customer-registry:001-create-contact-table
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'cr_contact'
CREATE TABLE IF NOT EXISTS cr_contact (
  id            UUID PRIMARY KEY,
  customer_id   UUID         NOT NULL REFERENCES cr_customer(id) ON DELETE CASCADE,
  contact_type  VARCHAR(10)  NOT NULL,
  contact_value VARCHAR(255) NOT NULL,
  is_primary    BOOLEAN      NOT NULL DEFAULT FALSE
);
--rollback DROP TABLE IF EXISTS cr_contact;

--changeset customer-registry:001-create-indexes
CREATE INDEX IF NOT EXISTS idx_cr_customer_document ON cr_customer(document_number);
CREATE INDEX IF NOT EXISTS idx_cr_customer_status   ON cr_customer(status);
CREATE INDEX IF NOT EXISTS idx_cr_customer_type     ON cr_customer(type);
CREATE INDEX IF NOT EXISTS idx_cr_address_customer  ON cr_address(customer_id);
CREATE INDEX IF NOT EXISTS idx_cr_contact_customer  ON cr_contact(customer_id);
CREATE INDEX IF NOT EXISTS idx_cr_customer_attrs    ON cr_customer USING GIN (attributes);
--rollback DROP INDEX IF EXISTS idx_cr_customer_document; DROP INDEX IF EXISTS idx_cr_customer_status; DROP INDEX IF EXISTS idx_cr_customer_type; DROP INDEX IF EXISTS idx_cr_address_customer; DROP INDEX IF EXISTS idx_cr_contact_customer; DROP INDEX IF EXISTS idx_cr_customer_attrs;
