# Changelog

All notable changes to the Customer Registry project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.0] - 2026-02-06

### Added

#### Backend (Spring Boot Starter)
- **Domain Model**: Customer aggregate with PF/PJ types, CPF/CNPJ validation, status state machine (DRAFT/ACTIVE/SUSPENDED/CLOSED)
- **Typed Attributes**: Sealed interface hierarchy (String, Integer, Boolean, Decimal, Date) stored as PostgreSQL JSONB with schemaVersion
- **Extension SPIs**: `CustomerValidator` and `CustomerEnricher` interfaces for host app customization
- **Service Pipeline**: validate → dedup → enrich → persist → publish event
- **AutoConfiguration**: Feature-flag-driven wiring with `@ConditionalOnProperty` and `@ConditionalOnMissingBean`
- **JPA Persistence**: PostgreSQL adapter with `cr_`-prefixed tables, JSONB attribute storage
- **Liquibase Migrations**: Versioned formatted-SQL changelogs with idempotent preconditions
- **JSONB Migration**: Advisory-lock-based schema migration with contention tracking
- **REST API**: CRUD endpoints at `/api/v1/customers` with RFC 7807 ProblemDetails
- **Domain Events**: `CustomerCreated`, `CustomerUpdated`, `CustomerStatusChanged` with deterministic eventId
- **Observability**: Micrometer metrics (operations, durations, schema versions, migration stats)
- **i18n**: Scoped MessageSource with pt-BR and en translations

#### Frontend (Angular Library)
- **Components**: CustomerForm, CustomerList, CustomerSearch, CustomerDetails (all Standalone + OnPush)
- **SafeFieldRendererHost**: Dynamic renderer creation with graceful degradation to Material fallback
- **Signal-based State**: CustomerStateService with reactive signals for customers, loading, error states
- **InjectionTokens**: CUSTOMER_EXTRA_FIELDS, CUSTOMER_VALIDATION_RULES, CUSTOMER_FIELD_RENDERERS, CUSTOMER_I18N_OVERRIDES
- **provideCustomerRegistry()**: Convenience function for library configuration
- **Validators**: CPF and CNPJ Angular ValidatorFn with full checksum algorithms
- **i18n**: Signal-based locale switching with fallback chain
- **Theming**: CSS custom properties (`--crui-*`) for host app customization

#### Infrastructure
- **Docker-only builds**: Java 21 + Maven, Node 20 + Angular CLI images
- **Makefile**: Docker-wrapped build, test, verify, security-scan commands
- **Azure DevOps Pipelines**: PR pipeline (build, test, architecture, security), Release pipeline (tag-triggered publish)
- **Security Scanning**: SpotBugs + FindSecBugs, Semgrep, OWASP Dependency-Check, npm audit, CycloneDX SBOM
- **Example Projects**: Backend (LoyaltyValidator, LoyaltyEnricher, EventLogger) and Frontend (custom renderer, buggy renderer, i18n overrides)
