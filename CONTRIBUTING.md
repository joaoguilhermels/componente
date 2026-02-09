# Contributing to Customer Registry

Thank you for considering contributing to the Customer Registry project.

## Prerequisites

- **Docker** and **Docker Compose** (all builds run in containers)
- **Make** (for build commands)
- **Git**

**IMPORTANT**: Never run Java, Maven, Node, or npm locally. All commands run inside Docker.

## Getting Started

```bash
# Clone the repository
git clone <repo-url> && cd onefinancial

# Build everything
make build

# Run all tests
make test

# Start infrastructure (Postgres + pgAdmin)
make up
```

## Development Workflow

### Branch Naming

| Prefix | Purpose | Example |
|--------|---------|---------|
| `feature/` | New features | `feature/add-email-validation` |
| `fix/` | Bug fixes | `fix/cpf-checksum-off-by-one` |
| `docs/` | Documentation only | `docs/update-api-reference` |
| `refactor/` | Code refactoring | `refactor/extract-validation-spi` |
| `test/` | Test additions/fixes | `test/add-enricher-edge-cases` |

### Workflow Steps

1. Create a branch from `main` with the appropriate prefix
2. Write tests first (TDD: red-green-refactor)
3. Implement the feature or fix
4. Run tests: `make test`
5. Run full verification: `make verify`
6. Open a pull request to `main`

## Running Tests

```bash
# All tests (Java + Angular)
make test

# Java tests only
make test-java

# Angular tests only
make test-angular

# Full verification (includes ArchUnit module boundary checks)
make verify

# Security scan
make security-scan
```

### Testcontainers (Integration Tests)

Integration tests that need a real database use Testcontainers. When running inside Docker-in-Docker:

```bash
docker compose -f docker/docker-compose.yml run --rm --no-deps \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -e TESTCONTAINERS_RYUK_DISABLED=true \
  -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal \
  java-build mvn test -pl customer-registry-starter
```

## Code Style and Conventions

### Java

- **Hexagonal architecture**: domain logic in `core/`, adapters in separate modules
- All public beans must use `@ConditionalOnMissingBean` so the host app can override
- All features gated by `@ConditionalOnProperty` (off by default)
- Bridge config pattern: public `@Configuration` classes expose package-private beans
- Never use `@ComponentScan` in auto-configuration (picks up test inner classes)
- DB table prefix: `cr_` (e.g., `cr_customer`, `cr_address`)
- Property prefix: `customer.registry.*`
- Feature flag prefix: `customer.registry.features.*`

### Angular

- Standalone components with `ChangeDetectionStrategy.OnPush`
- Signal-based state management
- Selector prefix: `crui-`
- CSS custom property prefix: `--crui-*`
- i18n: ship `pt-BR` and `en`, allow host overrides

### Commit Messages

Use [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <description>

[optional body]

[optional footer(s)]
```

| Type | Description |
|------|-------------|
| `feat` | New feature |
| `fix` | Bug fix |
| `docs` | Documentation |
| `refactor` | Code restructuring (no behavior change) |
| `test` | Adding or fixing tests |
| `chore` | Build, CI, tooling changes |

Examples:
```
feat(core): add email validation to Contact model
fix(persistence): handle null JSONB attributes on read
docs(readme): update feature flag table
test(rest): add missing PATCH endpoint tests
```

## Adding a New Module

1. Create the module directory under `customer-registry-starter/src/main/java/com/onefinancial/customer/<module>/`
2. Add a `package-info.java` with the appropriate Spring Modulith `@ApplicationModule` annotation
3. Only depend on `core` (never on other adapter modules)
4. Create a bridge configuration class if beans need to be exposed to auto-config
5. Add a `@AutoConfiguration` class in `autoconfigure/` with proper `@ConditionalOnProperty`
6. Register the auto-configuration in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
7. Add tests: unit tests for the module, integration test for auto-config wiring
8. Update the ArchUnit test if module boundary rules change
9. Document the new module in `README.md`

## Pull Request Process

1. Fill out the PR template completely
2. Ensure all CI checks pass (tests, ArchUnit, security scan)
3. Request review from at least one code owner
4. Address all review comments
5. Squash-merge into `main`

### PR Checklist

- [ ] Tests pass (`make test`)
- [ ] Full verification passes (`make verify`)
- [ ] No new security warnings
- [ ] Documentation updated (if applicable)
- [ ] No breaking changes (or documented in PR description)
- [ ] Follows naming conventions and architecture rules

## Deprecation Policy

- Deprecated features are marked with `@Deprecated` (Java) or `@deprecated` JSDoc (TypeScript) in version **N**
- A deprecation warning is logged at runtime when deprecated code paths are used
- Deprecated features are removed no earlier than version **N+2**
- The CHANGELOG must document all deprecations and removals
- Breaking changes (including removals of deprecated APIs) require a **major** version bump

## Architecture Overview

```
core/          Domain model, ports, SPIs, service (NO infrastructure deps)
persistence/   JPA adapter (depends on core only)
rest/          REST controller (depends on core only)
events/        Event publishing adapter (depends on core only)
observability/ Metrics and spans (depends on core only)
migration/     Schema migration (depends on core + persistence)
autoconfigure/ Conditional wiring for all modules
```

See [README.md](README.md) for detailed architecture diagrams, domain model, REST API reference, event schemas, and observability metrics.
