# 1ff Customer Registry — Consolidated Specialist Review Report

**Date:** 2026-02-08
**Branch:** `feature/phase-9-cicd-docs`
**Reviewers:** 8 specialist agents (backend architecture, database, security, frontend, testing, DevOps, code quality, AI DX)

---

## Executive Summary

The 1ff Customer Registry library is **well-architected** with clean hexagonal architecture, proper Spring Modulith boundaries, and good extensibility via SPI. However, the review uncovered **several issues that should be addressed before production use or merge to main**.

**Total findings: 73**

| Severity | Count | Must-fix before merge? |
|----------|-------|----------------------|
| Critical | 7 | YES |
| High | 18 | YES (most) |
| Medium | 29 | Recommended |
| Low | 19 | Nice-to-have |

The top cross-cutting concerns are:
1. **Missing `@Transactional` boundaries** (flagged by 3 reviewers independently)
2. **PII leakage** in events, exceptions, toString(), and API responses
3. **REST API contract mismatch** — documented/frontend endpoints that don't exist in backend
4. **No pagination** on `findAll()` — OOM and DoS risk
5. **Docker security** — root containers, hardcoded credentials, unmounted socket by default

---

## Critical Findings (Must Fix)

### CRIT-1: No `@Transactional` Boundaries on Write Operations
- **Flagged by:** backend-architect, dba-advisor, code-quality-reviewer (3 independent reviewers)
- **File:** `CustomerRegistryService.java`
- **Impact:** `register()`, `update()`, `changeStatus()` perform save + event publish without transaction. If event publish fails after save, data is persisted but event is lost. With Spring Modulith event outbox, the outbox entry and domain write MUST share a transaction. Also: `changeStatus()` does read-then-write without transaction — vulnerable to lost updates under concurrency.
- **Fix:** Add `@Transactional` at the service layer (or via a transactional proxy in auto-config since service has no Spring annotations by design). Read-only methods should use `@Transactional(readOnly = true)`.

### CRIT-2: REST API Contract Mismatch — DELETE Endpoint Missing
- **Flagged by:** code-quality-reviewer
- **Files:** `CustomerController.java`, `CustomerRegistryApiClient` (Angular), `README.md`
- **Impact:** Angular client calls `DELETE /api/v1/customers/{id}`, README documents it, but the backend controller has no delete endpoint and `CustomerRepository` port has no delete method. Consumers get **405 Method Not Allowed** at runtime.
- **Fix:** Either implement the delete endpoint end-to-end (port → adapter → service → controller) or remove it from the Angular client and documentation.

### CRIT-3: Hardcoded Database Credentials in docker-compose.yml
- **Flagged by:** devops-lead, security-lead, code-quality-reviewer
- **File:** `docker/docker-compose.yml`
- **Impact:** `POSTGRES_PASSWORD: crpassword` and `PGADMIN_DEFAULT_PASSWORD: admin` in plain text, committed to source control.
- **Fix:** Use `.env` file (already gitignored) with `env_file:` directive. Provide `.env.example` as documentation.

### CRIT-4: Docker Socket Mounted by Default
- **Flagged by:** devops-lead
- **File:** `docker/docker-compose.yml:36`
- **Impact:** `/var/run/docker.sock` always mounted for `java-build`, giving root-equivalent host access even when Testcontainers aren't needed.
- **Fix:** Use Docker Compose profiles (`profiles: [testcontainers]`) or a separate override file.

### CRIT-5: All Build Containers Run as Root
- **Flagged by:** devops-lead, security-lead
- **Files:** `docker/Dockerfile.java`, `docker/Dockerfile.node`
- **Impact:** Unnecessary privilege escalation risk. Supply chain attacks through dependencies have unrestricted container access.
- **Fix:** Add non-root user: `RUN addgroup -S builder && adduser -S builder -G builder` + `USER builder`.

### CRIT-6: No Project-Level CLAUDE.md
- **Flagged by:** ai-dx-specialist
- **Impact:** No version-controlled AI assistant configuration. All accumulated knowledge (learned patterns, gotchas, build commands) exists only in private `~/.claude` and is invisible to other developers or CI.
- **Fix:** Create `/CLAUDE.md` at project root with architecture rules, build commands, conventions, and gotchas.

### CRIT-7: Angular Package Version Mismatch (0.0.1 vs 0.1.0)
- **Flagged by:** code-quality-reviewer
- **Files:** `frontend/projects/customer-registry-ui/package.json` (0.0.1), `VERSION` (0.1.0), Maven POM (0.1.0-SNAPSHOT)
- **Impact:** Consumer confusion when trying to match backend and frontend library versions.
- **Fix:** Synchronize to `0.1.0` across all manifests.

---

## High Findings (Should Fix)

### Architecture & Design

| ID | Finding | Reviewer | File |
|----|---------|----------|------|
| H-ARCH-1 | `CustomerUpdated` event ID is non-deterministic (`Instant.now()` in seed), defeating stated idempotency | backend-architect | `CustomerUpdated.java:21` |
| H-ARCH-2 | PATCH endpoint mixes displayName + status change — displayName update silently lost when both provided | backend-architect, test-analyst | `CustomerController.java:81-100` |
| H-ARCH-3 | `IllegalArgumentException` for "not found" breaks exception hierarchy; string matching in handler | backend-architect, code-quality-reviewer | `CustomerRegistryService.java:81` |
| H-ARCH-4 | No `@ConfigurationPropertiesScan` or `additional-spring-configuration-metadata.json` for IDE hints | code-quality-reviewer | Auto-config |
| H-ARCH-5 | Feature flags documented in README don't match Angular `CustomerRegistryUiFeatures` interface | code-quality-reviewer | README.md vs code |

### Security & PII

| ID | Finding | Reviewer | File |
|----|---------|----------|------|
| H-SEC-1 | PII leakage: CPF/CNPJ in `CustomerCreated` event payload, `Customer.toString()`, example logger | security-lead | Multiple |
| H-SEC-2 | `DuplicateDocumentException` exposes full document number in error response — enumeration attack | security-lead | `DuplicateDocumentException.java` |
| H-SEC-3 | Full CPF/CNPJ returned in `findAll()` API response — bulk PII extraction | security-lead | `CustomerResponse.java` |

### Performance

| ID | Finding | Reviewer | File |
|----|---------|----------|------|
| H-PERF-1 | `findAll()` returns unbounded result set — no pagination, OOM risk | dba-advisor, security-lead | `CustomerRepository.java`, `CustomerController.java` |
| H-PERF-2 | N+1 query on addresses/contacts: `FetchType.LAZY` but mapper always accesses both | dba-advisor | `CustomerEntity.java:49-53` |

### Testing

| ID | Finding | Reviewer | File |
|----|---------|----------|------|
| H-TEST-1 | Advisory lock concurrency test uses `Thread.sleep` timing — flaky | test-analyst | `AttributeMigrationServiceIntegrationTest.java:242` |
| H-TEST-2 | No unit tests for `AttributesJsonConverter` serialization (5 types, edge cases) | test-analyst | `AttributesJsonConverter.java` |
| H-TEST-3 | No unit tests for `CustomerEntityMapper` bidirectional mapping | test-analyst | `CustomerEntityMapper.java` |
| H-TEST-4 | `CustomerValidationException` path through REST layer untested | test-analyst | `CustomerExceptionHandler.java` |
| H-TEST-5 | `InMemoryCustomerRepository` has no direct tests | test-analyst | `InMemoryCustomerRepository.java` |

### Frontend

| ID | Finding | Reviewer | File |
|----|---------|----------|------|
| H-FE-1 | Subscription leak in `SafeFieldRendererHostComponent.activateFallback()` — never unsubscribed | frontend-architect | `safe-field-renderer-host.component.ts:139` |
| H-FE-2 | `CustomerStateService` writable signals exposed publicly — consumers can bypass state management | frontend-architect | `customer-state.service.ts:17-35` |

### DevOps

| ID | Finding | Reviewer | File |
|----|---------|----------|------|
| H-OPS-1 | Node Docker image not pinned to digest | devops-lead | `Dockerfile.node` |
| H-OPS-2 | Semgrep installed via unpinned `pip install` | devops-lead | `security-scan.yml:23` |

---

## Medium Findings (29 total)

### Architecture & Design (7)
| ID | Finding | Reviewer |
|----|---------|----------|
| M-ARCH-1 | Observability auto-config has no dedicated feature flag (inconsistent with other modules) | backend-architect |
| M-ARCH-2 | `CustomerSearchComponent.isVisible` getter never used in template — feature flag not enforced | frontend-architect |
| M-ARCH-3 | `SafeFieldRendererHostComponent` context key uses control value instead of field key | frontend-architect |
| M-ARCH-4 | `AttributesJsonConverter` naming misleading — utility class masquerading as JPA converter | dba-advisor |
| M-ARCH-5 | REST DTOs public but controller package-private — inconsistent visibility | code-quality-reviewer |
| M-ARCH-6 | BOM too thin — only declares starter, not transitive dependencies | code-quality-reviewer |
| M-ARCH-7 | `provideCustomerRegistry()` called multiple times loses config (non-multi token) | frontend-architect |

### Security (5)
| ID | Finding | Reviewer |
|----|---------|----------|
| M-SEC-1 | No pagination = DoS vector | security-lead |
| M-SEC-2 | No `@Size` constraints on `CreateCustomerRequest` fields | security-lead |
| M-SEC-3 | Missing exception handlers for `HttpMessageNotReadableException`, `MethodArgumentTypeMismatchException` | security-lead |
| M-SEC-4 | Docker images not pinned to SHA256 digest | security-lead |
| M-SEC-5 | Semgrep SQL injection rules have limited pattern coverage | security-lead |

### Performance (2)
| ID | Finding | Reviewer |
|----|---------|----------|
| M-PERF-1 | `save()` always creates detached entity — extra SELECT + full collection replace | dba-advisor |
| M-PERF-2 | Advisory lock holds 2 connections during migration | dba-advisor |

### Testing (8)
| ID | Finding | Reviewer |
|----|---------|----------|
| M-TEST-1 | Missing ACTIVE→CLOSED and SUSPENDED→CLOSED status transition tests | test-analyst |
| M-TEST-2 | No test for self-transition (same status) | test-analyst |
| M-TEST-3 | No test for `findByDocument` service method | test-analyst |
| M-TEST-4 | Angular detail/form tests emit events directly instead of clicking UI | test-analyst |
| M-TEST-5 | No Angular tests for HTTP error handling in API client | test-analyst |
| M-TEST-6 | `CustomerStateService` error handling paths inconsistent | test-analyst |
| M-TEST-7 | No Angular test for custom `apiBaseUrl` | test-analyst |
| M-TEST-8 | Raw JDBC migration bypasses Hibernate cache | dba-advisor |

### DevOps (4)
| ID | Finding | Reviewer |
|----|---------|----------|
| M-OPS-1 | Missing `.dockerignore` | devops-lead |
| M-OPS-2 | Makefile `java-build` always depends on Postgres (no `--no-deps`) | devops-lead |
| M-OPS-3 | Maven cache key misses sub-module POMs | devops-lead |
| M-OPS-4 | `npm audit --production` skips devDependencies | devops-lead |

### Documentation & Quality (3)
| ID | Finding | Reviewer |
|----|---------|----------|
| M-DOC-1 | No JSDoc on Angular component `@Input`/`@Output` properties | code-quality-reviewer |
| M-DOC-2 | No deprecation annotations or policy implementation despite README policy | code-quality-reviewer |
| M-DOC-3 | TranslatePipe is `pure: false` — performance concern with many bindings | frontend-architect |

---

## Low Findings (19 total)

<details>
<summary>Click to expand low-severity findings</summary>

| ID | Finding | Reviewer |
|----|---------|----------|
| L-1 | Fragile length-based heuristic for document type in findByDocument | backend-architect |
| L-2 | No migration chain contiguity validation in AttributeMigrationService | backend-architect |
| L-3 | InMemoryCustomerRepository no deep-copy (diverges from JPA semantics) | backend-architect |
| L-4 | Redundant index on document_number (unique constraint already creates one) | dba-advisor |
| L-5 | `state VARCHAR(2)` limits internationalization | dba-advisor |
| L-6 | No `@Version` optimistic locking | dba-advisor |
| L-7 | Liquibase `includeAll` classpath resolution may conflict in multi-JAR | dba-advisor |
| L-8 | `createdAt`/`updatedAt` not auto-managed by JPA callbacks | dba-advisor |
| L-9 | No rate limiting or request size limits documented | security-lead |
| L-10 | SpotBugs exclusion blanket-suppresses entire model package | security-lead |
| L-11 | Advisory lock key is static/hardcoded | security-lead |
| L-12 | OWASP threshold at CVSS 7 may miss medium-severity PII exploits | security-lead |
| L-13 | `PublishBuildArtifacts@1` deprecated in Azure DevOps | devops-lead |
| L-14 | No `restoreKeys` in pipeline cache tasks | devops-lead |
| L-15 | Makefile missing `install-angular` target | devops-lead |
| L-16 | Frontend README is auto-generated Angular CLI boilerplate | code-quality-reviewer |
| L-17 | `public-api.ts` uses `export *` — accidental API exposure risk | code-quality-reviewer |
| L-18 | Inconsistent i18n key naming between backend and frontend | code-quality-reviewer |
| L-19 | No `.github/copilot-instructions.md` or `.cursorrules` | ai-dx-specialist |

</details>

---

## Positive Observations

Across all 8 reviews, the following strengths were consistently noted:

1. **Clean hexagonal architecture** — Core module has zero infrastructure dependencies, adapters depend inward only, module boundaries are correctly enforced with Spring Modulith.
2. **Well-designed SPI** — `CustomerValidator`, `CustomerEnricher`, `AttributeSchemaMigration` are clean extension points.
3. **Bridge config pattern** correctly avoids `@ComponentScan` pitfalls.
4. **Secure-by-default** — All features disabled until explicitly enabled via properties.
5. **Comprehensive CI security scanning** — SpotBugs + Semgrep + OWASP DC + npm audit + CycloneDX SBOM.
6. **Robust CPF/CNPJ validation** — Checksum verification, no injection possible through document values.
7. **All SQL uses parameterized queries** — No SQL injection risk.
8. **RFC 9457 ProblemDetail error responses** with i18n support.
9. **Angular library well-structured** — OnPush everywhere, signal-based state, graceful degradation, tree-shakeable providers.
10. **Consistent naming conventions** — `cr_` DB prefix, `crui-` Angular selectors, `customer.registry.*` properties.

---

## Recommended Fix Priority

### Phase A: Must-fix before merge (Critical + High blockers)

| Priority | ID | Finding | Effort |
|----------|----|---------|--------|
| 1 | CRIT-1 | Add `@Transactional` boundaries | Medium |
| 2 | CRIT-2 | Fix REST API contract (implement or remove delete) | Medium |
| 3 | H-SEC-1,2,3 | PII masking in events, exceptions, toString, responses | Medium |
| 4 | H-FE-1 | Fix subscription leak in SafeFieldRendererHostComponent | Low |
| 5 | H-PERF-1 | Add pagination to findAll | Medium |
| 6 | CRIT-3,4,5 | Docker security (credentials, socket, root) | Low |
| 7 | H-ARCH-2 | Fix PATCH endpoint displayName + status interaction | Low |
| 8 | H-ARCH-3 | Replace IllegalArgumentException with CustomerNotFoundException | Low |
| 9 | CRIT-7 | Synchronize Angular package version | Trivial |
| 10 | H-FE-2 | Use `asReadonly()` for state service signals | Low |

### Phase B: Should fix soon after merge

| Priority | ID | Finding | Effort |
|----------|----|---------|--------|
| 11 | H-PERF-2 | Fix N+1 with @BatchSize or @EntityGraph | Low |
| 12 | H-TEST-1 | Fix flaky advisory lock test | Low |
| 13 | H-TEST-2,3 | Add unit tests for converter and mapper | Medium |
| 14 | CRIT-6 | Create project-level CLAUDE.md | Medium |
| 15 | H-ARCH-1 | Fix CustomerUpdated event ID determinism | Low |
| 16 | H-ARCH-5 | Align README feature flags with code | Low |

### Phase C: Iterative improvements

All Medium and Low findings, prioritized by consumer impact.

---

## Cross-Cutting Themes

### Theme 1: Transaction Safety
Three reviewers independently flagged the missing `@Transactional`. This is the single most impactful issue — it affects data consistency, event reliability, and concurrent access safety.

### Theme 2: PII Discipline
CPF/CNPJ (Brazilian SSN equivalents) appear unmasked in 5+ locations. For LGPD compliance, a systematic PII masking strategy is needed across events, exceptions, logging, and API responses.

### Theme 3: API Contract Integrity
The frontend client, README documentation, and backend implementation are not fully aligned. The delete endpoint gap will cause immediate runtime failures for consumers.

### Theme 4: Pagination
Every reviewer who touched the data path flagged the unbounded `findAll()` — it's both a performance and security concern.

### Theme 5: Docker Hardening
Build containers running as root with hardcoded credentials and always-mounted Docker socket is a significant security posture gap for a library handling PII.

---

*Report generated by 8-specialist parallel review team on 2026-02-08*
