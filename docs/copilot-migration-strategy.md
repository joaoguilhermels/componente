# Copilot Chat Migration Strategy

> **Purpose**: Step-by-step guide for using GitHub Copilot Chat (VS Code) to migrate
> legacy Spring Boot services to the OneFinancial hexagonal architecture pattern.
>
> **Approach**: Multi-root workspace with three directories -- reference, legacy, and target.
> Copilot reads patterns from the reference, analyzes the legacy, and generates code in the target.
>
> **Related documents**:
> - [Migration Guide](migration-guide.md) -- Architecture and patterns reference
> - [Copilot Prompts Library](../migration/copilot-prompts.md) -- Ready-to-paste prompts
> - [Migration Scorecard](../migration/scorecard.md) -- CI-automatable progress tracker
> - [Lessons Learned Template](../migration/template/MIGRATION-LESSONS.md.template) -- Post-migration feedback

---

## Table of Contents

- [Phase Mapping](#phase-mapping)
- [Prerequisites](#prerequisites)
- [Step 1: Setup the Workspace](#step-1-setup-the-workspace)
- [Step 2: Phase 0 -- Legacy Analysis](#step-2-phase-0----legacy-analysis)
- [Step 3: Phase 1 -- Scaffold](#step-3-phase-1----scaffold)
- [Step 4: Phase 2 -- Core Extraction](#step-4-phase-2----core-extraction)
- [Step 5: Phase 3 -- Adapters](#step-5-phase-3----adapters)
- [Step 6: Phase 4 -- Auto-Configuration](#step-6-phase-4----auto-configuration)
- [Step 7: Phase 5 -- Frontend (Optional)](#step-7-phase-5----frontend-optional)
- [Step 8: Verification](#step-8-verification)
- [Step 9: Lessons Learned](#step-9-lessons-learned)
- [Troubleshooting](#troubleshooting)
- [Tips for Effective Copilot Chat Usage](#tips-for-effective-copilot-chat-usage)

---

## Phase Mapping

This workflow introduces **Phase 0 (Legacy Analysis)** which does not exist in the
[Migration Scorecard](../migration/scorecard.md). The scorecard phases map as follows:

| Copilot Workflow | Scorecard | Notes |
|------------------|-----------|-------|
| Phase 0 -- Legacy Analysis | (no scorecard equivalent) | Pre-migration analysis, not scored |
| Phase 1 -- Scaffold | Phase 1 -- Foundation (25%) | Package structure, marker class, CI |
| Phase 2 -- Core Extraction | Phase 2 -- Core Domain (35%) | Domain model, ports, services |
| Phase 3 -- Adapters | Phase 3 -- Adapters (15%) | Bridge configs, integration tests |
| Phase 4 -- Auto-Configuration | Phase 4 -- Auto-configuration (20%) | Feature flags, conditional beans |
| Phase 5 -- Frontend | Phase 5 -- Frontend (5%) | Angular library (optional) |

Phase 0 output (`LEGACY-ANALYSIS.md` and `MIGRATION-PLAN.md`) feeds into Phase 1
but is not tracked in the scorecard since it produces analysis documents, not code.

---

## Prerequisites

### Tools Required

| Tool | Version | Purpose |
|------|---------|---------|
| VS Code | Latest | IDE with Copilot Chat |
| GitHub Copilot Chat | Latest | AI-assisted code generation |
| Docker Desktop | Latest | Build environment (never run Java/Node locally) |
| Git | 2.x+ | Version control |

### Knowledge Required

- Familiarity with hexagonal architecture concepts (see [Migration Guide, Section 3](migration-guide.md))
- Understanding of Spring Modulith module boundaries
- Access to the legacy service source code
- Access to the OneFinancial Customer Registry reference repository

### Before You Start

1. Clone the OneFinancial Customer Registry reference repository
2. Clone (or have access to) the legacy service repository
3. Create an empty directory for the migration target
4. Verify Docker Desktop is running

---

## Step 1: Setup the Workspace

### 1.1 Create the Multi-Root Workspace

Create a VS Code multi-root workspace with three directories:

```json
// migration-workspace.code-workspace
{
  "folders": [
    {
      "path": "./reference",
      "name": "reference"
    },
    {
      "path": "./legacy",
      "name": "legacy"
    },
    {
      "path": "./target",
      "name": "target"
    }
  ],
  "settings": {
    "github.copilot.chat.codeGeneration.instructions": [
      {
        "file": "reference/migration/template/.github/instructions/migration-workflow.instructions.md"
      }
    ]
  }
}
```

### 1.2 Populate the Directories

```bash
# Create workspace directory
mkdir migration-workspace && cd migration-workspace

# Clone the reference repository
git clone <onefinancial-repo-url> reference

# Clone the legacy service
git clone <legacy-service-url> legacy

# Create empty target directory
mkdir target

# Copy the workspace file
# (create migration-workspace.code-workspace as shown above)
```

### 1.3 Copy Instruction Files to Target

```bash
# Copy the Copilot instruction files
cp -r reference/migration/template/.github target/.github

# Copy the lessons learned template
cp reference/migration/template/MIGRATION-LESSONS.md.template target/MIGRATION-LESSONS.md
```

### 1.4 Verify Workspace Setup

Open the `.code-workspace` file in VS Code. You should see three root folders in the
explorer: `reference`, `legacy`, and `target`. Copilot Chat should be available in the
sidebar.

**Checkpoint**: The workspace has three directories visible. The `target/.github/instructions/`
directory contains the migration workflow instruction file.

---

## Step 2: Phase 0 -- Legacy Analysis

### Goal

Understand the legacy service structure before writing any code.

### Process

1. Open Copilot Chat in VS Code
2. Use Prompt 0.1 from the [Copilot Prompts Library](../migration/copilot-prompts.md#prompt-01-analyze-legacy-service-structure)
3. Replace `<placeholders>` with actual values from the legacy service
4. Use `@workspace` to give Copilot visibility into the legacy source code

### How to Reference Files in Copilot Chat

Copilot Chat supports file references using `#file:` syntax:

```
#file:legacy/src/main/java/com/example/billing/BillingService.java
```

And workspace-level queries using `@workspace`:

```
@workspace What JPA entities exist in the legacy directory?
```

### Verification

- [ ] Entity inventory matches actual `@Entity` classes in legacy code
- [ ] All external dependencies are identified
- [ ] Tier classification makes sense (Simple/Standard/Advanced)
- [ ] Analysis report saved as `target/LEGACY-ANALYSIS.md`

### After This Phase

Record observations in `target/MIGRATION-LESSONS.md`, Phase 0 section:
- Was the analysis accurate?
- Did Copilot miss any entities or dependencies?
- Were there unexpected patterns in the legacy code?

---

## Step 3: Phase 1 -- Scaffold

### Goal

Create the package structure, marker class, and architectural gate tests.

### Process

1. Use Prompt 1.1 from the [Copilot Prompts Library](../migration/copilot-prompts.md#prompt-11-generate-package-structure-and-marker-class)
2. Replace all `<PLACEHOLDER>` values with your service's actual values:

| Placeholder | Your Value | Example |
|-------------|------------|---------|
| `<SERVICE_NAME_PASCAL>` | | `BillingService` |
| `<BASE_PACKAGE>` | | `com.onefinancial.billing` |
| `<PACKAGE_PATH>` | | `com/onefinancial/billing` |
| `<DB_PREFIX>` | | `bs_` |
| `<PROPERTY_PREFIX>` | | `billing.service` |
| `<ANGULAR_PREFIX>` | | `bsui` |

3. Review the generated files carefully before saving
4. Use Prompt 1.2 for build configuration (Makefile, docker-compose, pom.xml)

### Verification

- [ ] Marker class uses `@Modulithic` (NOT `@SpringBootApplication`)
- [ ] Core `package-info.java` has `@ApplicationModule(type = Type.OPEN)`
- [ ] Other modules have `allowedDependencies = {"core"}`
- [ ] `ModulithStructureTest` references the correct marker class
- [ ] Build configuration uses Docker (not local Java/Node)

### CLI Verification

```bash
# Run automated scorecard checks for Phase 1
python3 scripts/migration_cli.py verify --phase 1 --target ./target
```

### After This Phase

Record in `target/MIGRATION-LESSONS.md`, Phase 1 section.

---

## Step 4: Phase 2 -- Core Extraction

### Goal

Extract pure domain model with zero infrastructure dependencies.

### Process

1. **Domain Model** (Prompt 2.1): For each legacy entity, generate a pure domain class
   - Show Copilot the legacy entity AND the reference `Customer.java`
   - Copilot reads the reference pattern, then transforms the legacy entity
   - Repeat for each entity in the legacy service

2. **Port Interfaces** (Prompt 2.2): For each external dependency, create a port
   - Show Copilot the legacy service class AND the reference `CustomerRepository.java`
   - Ports use only domain types -- no JPA, no Spring Data

3. **Service Layer** (Prompt 2.3): Create the domain service
   - Show Copilot the legacy service AND the reference `CustomerRegistryService.java`
   - No `@Service` annotation -- wired by auto-config
   - `@Transactional` is the only allowed Spring annotation in core services

4. **Unit Tests** (Prompt 2.4): Write tests for model and service
   - Pure unit tests -- no Spring context needed

### Verification

Run this check (manually or ask Copilot):

```bash
# Must return zero results
grep -r "import jakarta\.\|import org\.springframework\.data\.\|import org\.springframework\.web\.\|import org\.springframework\.stereotype\." \
  target/src/main/java/**/core/
```

- [ ] Zero infrastructure imports in `core/` (except `@Transactional` and `@Modulithic`)
- [ ] All entities have named static factories
- [ ] PII is masked in `toString()` methods
- [ ] Port interfaces use only domain types
- [ ] Service has no Spring stereotype annotations
- [ ] Unit tests pass without Spring context

### CLI Verification

```bash
# Run automated scorecard checks for Phase 2
python3 scripts/migration_cli.py verify --phase 2 --target ./target
```

### After This Phase

Record in `target/MIGRATION-LESSONS.md`, Phase 2 section. Pay special attention to
whether JPA annotations were properly removed and whether port interface design was correct.

---

## Step 5: Phase 3 -- Adapters

### Goal

Create infrastructure adapters with bridge configuration pattern.

### Process

1. **Persistence Adapter** (Prompt 3.1):
   - JPA entity (separate from domain model)
   - Separate entity mapper utility class (static `toEntity()`/`toDomain()` methods)
   - Spring Data repository (package-private)
   - Adapter implementing the core port (package-private)
   - Bridge config (public `@Configuration`)

2. **REST Adapter** (Prompt 3.2):
   - Request/Response DTOs (records)
   - Controller (package-private)
   - Bridge config (public `@Configuration`)

3. **Events Adapter** (Prompt 3.3):
   - Event publisher implementing core port (package-private)
   - Bridge config (public `@Configuration`)

4. **Adapter Tests** (Prompt 3.4):
   - Integration tests for persistence (with `@DataJpaTest`)
   - Unit tests for REST (with `@WebMvcTest`)
   - Unit tests for events

### Verification

- [ ] Bridge config files exist for each adapter module (persistence, REST, events)
- [ ] Bridge configs are `public` classes
- [ ] Adapters and controllers are package-private
- [ ] JPA entities implement `Persistable<UUID>`
- [ ] `@JdbcTypeCode(SqlTypes.JSON)` used for JSONB (not `@Convert`)
- [ ] `@BatchSize(size=25)` on `@OneToMany` collections
- [ ] No PII in events

### CLI Verification

```bash
# Run automated scorecard checks for Phase 3
python3 scripts/migration_cli.py verify --phase 3 --target ./target
```

### After This Phase

Record in `target/MIGRATION-LESSONS.md`, Phase 3 section.

---

## Step 6: Phase 4 -- Auto-Configuration

### Goal

Wire all modules with conditional, overridable configuration.

### Process

1. **Auto-Config Classes** (Prompt 4.1):
   - Events auto-config (runs BEFORE core)
   - Core auto-config (creates service bean, provides fallbacks)
   - Persistence auto-config (runs AFTER core)
   - REST auto-config (runs AFTER core)
   - META-INF registration file

2. **Properties Class** (Prompt 4.2):
   - `@ConfigurationProperties` with nested Features
   - All features default to `false`

3. **Auto-Config Tests** (Prompt 4.3):
   - `ApplicationContextRunner` tests for each auto-config
   - Test enabled/disabled states
   - Test `@ConditionalOnMissingBean` overridability

### Verification

```bash
# Verify no matchIfMissing = true anywhere (secure-by-default)
grep -r "matchIfMissing.*true" target/src/main/java/**/autoconfigure/
# Expected: zero results

# Verify dual-gate on adapter auto-configs
grep -A3 '@ConditionalOnProperty' target/src/main/java/**/autoconfigure/*AutoConfiguration.java
# Expected: adapter auto-configs show name = {"enabled", "features.<name>"}
```

- [ ] Adapter auto-configs use dual-gate: `name = {"enabled", "features.<name>"}`
- [ ] Core auto-config gated only by master switch `<prefix>.enabled`
- [ ] No `matchIfMissing = true` (defaults to `false` when omitted, which is correct)
- [ ] `@ConditionalOnMissingBean` on all fallback beans (core) and bridge config beans
- [ ] Events auto-config ordered before core
- [ ] META-INF registration file lists all auto-configs
- [ ] Structured header comments on all auto-config classes
- [ ] Fallback beans exist (in-memory repo, no-op publisher)

### CLI Verification

```bash
# Run automated scorecard checks for Phase 4
python3 scripts/migration_cli.py verify --phase 4 --target ./target
```

### After This Phase

Record in `target/MIGRATION-LESSONS.md`, Phase 4 section.

---

## Step 7: Phase 5 -- Frontend (Optional)

> Skip this phase for backend-only services (Simple/Standard tier without UI).

### Goal

Migrate Angular components to library pattern.

### Process

1. **Library Structure** (Prompt 5.1):
   - Angular workspace with Jest configuration
   - Public API barrel with at least one export

2. **Component Migration** (Prompt 5.2):
   - Convert to standalone components with `OnPush`
   - Convert imperative state to signals
   - Apply i18n fallback chain

### Verification

```bash
# Build the Angular library (via Docker)
docker compose -f target/docker/docker-compose.yml run --rm node-build \
  ng build <angular-lib-name>
```

- [ ] All components are standalone
- [ ] All components use `ChangeDetectionStrategy.OnPush`
- [ ] Signal-based state with `asReadonly()` for public API
- [ ] CSS custom properties use `--<prefix>-*` naming
- [ ] No `@Input()` named `formControl`
- [ ] `public-api.ts` has at least one export

### CLI Verification

```bash
# Run automated scorecard checks for Phase 5
python3 scripts/migration_cli.py verify --phase 5 --target ./target

# Or run all phases at once for final verification
python3 scripts/migration_cli.py verify --target ./target
```

### After This Phase

Record in `target/MIGRATION-LESSONS.md`, Phase 5 section.

---

## Step 8: Verification

### Goal

Comprehensive review of the migrated codebase against all architectural rules.

### Process

1. **Self-Review** (Prompt V.1): Ask Copilot to review its own output against the scorecard
2. **Architecture Verification** (Prompt V.2): Run verification commands

### Checklist

| Dimension | Check | Status |
|-----------|-------|--------|
| Core isolation | Zero infra imports in core/ | |
| Module boundaries | All package-info.java correct | |
| Bridge configs | Public configs, private adapters | |
| Bean overridability | @ConditionalOnMissingBean on fallback + bridge beans | |
| Dual-gate pattern | Adapter auto-configs require enabled AND feature flag | |
| Feature flags | No matchIfMissing = true (false is the default) | |
| Entity mapper | Separate mapper class, not methods on JPA entity | |
| Auto-config ordering | Events before core | |
| META-INF | All auto-configs registered | |
| Modulith test | Compiles and passes | |
| PII protection | No PII in events/toString/exceptions | |
| No @ComponentScan | @Import used instead | |
| Controllers | Package-private | |
| Persistable | JPA entities implement Persistable<UUID> | |
| JSONB | @JdbcTypeCode used (not @Convert) | |

### Scorecard Gate

Calculate the scorecard score using the [Migration Scorecard](../migration/scorecard.md).
The migration is complete when the score reaches **95%** (Angular phase is optional
for backend-only services).

---

## Step 9: Lessons Learned

### Goal

Generate a structured post-migration report for continuous improvement.

### Process

1. Use Prompt L.1 from the [Copilot Prompts Library](../migration/copilot-prompts.md#prompt-l1-generate-migration-lessons-learned)
2. Ask Copilot to fill in the `target/MIGRATION-LESSONS.md` template
3. Review the generated report for completeness

### What to Record

The lessons learned document captures:

- **Decisions**: What architectural choices Copilot made and whether they were correct
- **Deviations**: Where generated code differed from reference patterns
- **Gaps**: Legacy patterns not covered by the current instructions
- **Improvements**: Specific suggestions for prompt and instruction file changes

### Feedback Loop

The lessons learned report feeds back into the migration framework:

```
Migration N generates MIGRATION-LESSONS.md
    -> Review identifies prompt/instruction improvements
        -> Update copilot-prompts.md and instruction files
            -> Migration N+1 benefits from improvements
```

After reviewing the lessons learned:

1. Open issues for suggested prompt changes (tag with `migration-improvement`)
2. Update instruction files if patterns are missing
3. Add new prompts for uncovered scenarios
4. Share findings with the team

---

## Troubleshooting

### Copilot Violates Hexagonal Boundaries

**Symptom**: Copilot puts JPA annotations in core domain model.

**Fix**: Include the `core-domain.instructions.md` file explicitly:
```
Read these rules first: #file:reference/migration/template/.github/instructions/core-domain.instructions.md

Now regenerate the domain model WITHOUT any JPA annotations.
The JPA entity is a SEPARATE class in the persistence/ module.
```

### Copilot Uses @SpringBootApplication

**Symptom**: Marker class has `@SpringBootApplication` instead of `@Modulithic`.

**Fix**: Remind Copilot this is a library:
```
This is a library module, not a standalone application.
Use @Modulithic annotation. See: #file:reference/migration/template/src/main/java/MarkerClass.java.template
```

### Copilot Makes Controllers Public

**Symptom**: Controller classes are declared `public`.

**Fix**:
```
Controllers MUST be package-private (default visibility).
The bridge configuration exposes them. Remove the 'public' modifier.
```

### Copilot Uses matchIfMissing = true

**Symptom**: Feature flags are enabled by default.

**Fix**:
```
SECURITY: All features must be OFF by default (secure-by-default).
Remove matchIfMissing = true — the default (false) is the correct behavior.
```

### Copilot Uses @ComponentScan

**Symptom**: Auto-config uses `@ComponentScan` instead of `@Import`.

**Fix**:
```
@ComponentScan picks up test inner classes and causes failures.
Replace @ComponentScan with @Import(BridgeConfiguration.class).
```

### Copilot Uses @Convert for JSONB

**Symptom**: JPA entity uses `@Convert` annotation for JSONB columns.

**Fix**:
```
Hibernate 6 does not support @Convert for JSONB properly.
Use @JdbcTypeCode(SqlTypes.JSON) instead.
```

### Copilot Chat Context is Too Large

**Symptom**: Copilot Chat returns incomplete or confused responses.

**Fix**: Break down the prompt into smaller pieces:
- Focus on one file at a time
- Use `#file:` for specific files instead of `@workspace`
- Start a new chat session for each phase

### Generated Code Does Not Compile

**Symptom**: Compilation errors in generated code.

**Fix**: Check for common issues:
- Missing `-parameters` compiler flag (Spring Boot 3.2+)
- Missing `ts-node` devDependency (Jest with .ts config)
- Empty `public-api.ts` (ng-packagr requires at least one export)
- Wrong import paths (check package names match)

---

## Tips for Effective Copilot Chat Usage

### 1. Always Show the Reference First

The most effective pattern is: **read reference -> analyze legacy -> generate code**.
Always include `#file:` references to the reference project so Copilot knows the
target patterns.

### 2. One File at a Time

Copilot Chat works best when generating one file per prompt. If you need multiple files,
use separate prompts for each and connect them with `#file:` references to previously
generated files.

### 3. Validate Before Moving On

Never proceed to the next phase without validating the current one. Use the verification
checklists in each phase section.

### 4. Use Recovery Prompts

When Copilot makes a mistake, use the specific recovery prompts from the
[Copilot Prompts Library](../migration/copilot-prompts.md). These are designed to correct
common errors without starting over.

### 5. Incrementally Add Instructions

Add instruction files to the target repository as you migrate each layer:
- Phase 1-2: `core-domain.instructions.md` + `tests.instructions.md`
- Phase 3: Add `adapters.instructions.md`
- Phase 4: Add `autoconfig.instructions.md`
- Phase 5: Add `angular.instructions.md`

This avoids overwhelming Copilot with rules for code that does not yet exist.

### 6. Record Lessons As You Go

Do not wait until the end to fill in `MIGRATION-LESSONS.md`. Record observations
after each phase while they are fresh. This produces a more accurate and useful report.

### 7. Start Fresh for Complex Phases

If Copilot Chat seems confused or is producing low-quality output, start a new chat
session. The context window has limits, and a fresh start with focused `#file:` references
often produces better results.
