## Summary

<!-- Brief description of what this PR does and why -->

## Changes

<!-- List the key changes made in this PR -->

-

## Migration Phase

<!-- If this PR is part of a migration, select the current phase -->
<!-- Delete this section if not applicable -->

| Phase | Description | Status |
|-------|-------------|--------|
| 1. Foundation | Project scaffold, Modulith marker, CI skeleton | |
| 2. Domain | Core model, ports, SPIs, service, domain events | |
| 3. Auto-Config | Auto-configuration classes, bridge configs, feature flags | |
| 4. Persistence | JPA adapter, Liquibase migrations, Testcontainers tests | |
| 5. REST/Events | REST controller, event publishing adapter, observability | |
| 6. Frontend Core | Angular library scaffold, i18n, state service, API client | |
| 7. Frontend Components | Search, list, detail, form components | |
| 8. Examples | Example client projects (backend + Angular) | |
| 9. CI/CD | Pipelines, security scanning, documentation | |

## Test Plan

<!-- How was this tested? Include relevant test commands and scenarios -->

- [ ] `make test` passes
- [ ] `make verify` passes (ArchUnit module boundary checks)
- [ ] Manual testing performed (describe below if applicable)

## Architecture Compliance

- [ ] `ModulithStructureTest` passes
- [ ] All modules have `package-info.java` with `@ApplicationModule`
- [ ] No cross-adapter dependencies (adapters depend inward on `core/` only)
- [ ] Bridge configs use `@Import`, not `@ComponentScan`
- [ ] Auto-config beans have `@ConditionalOnMissingBean`
- [ ] Auto-config classes have structured header comments
- [ ] Events auto-config runs before core (`@AutoConfiguration(before = ...)`)
- [ ] Feature flags are OFF by default (`havingValue = "true"`)
- [ ] No infrastructure imports in `core/` package

## Checklist

- [ ] Tests added/updated for the changes
- [ ] Documentation updated (README, CLAUDE.md, etc.)
- [ ] No breaking changes (or documented above)
- [ ] Follows project naming conventions (`cr_` tables, `crui-` selectors, `customer.registry.*` properties)
- [ ] No infrastructure dependencies added to `core/` module
- [ ] PII is masked in toString(), events, and exceptions
- [ ] Security scan clean (`make security-scan`)
