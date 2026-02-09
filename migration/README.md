# Migration Templates

This directory contains templates and guidelines for migrating existing services
to the OneFinancial hexagonal architecture pattern (Spring Modulith + Angular library).

## Directory Structure

```
migration/
  README.md                  # This file
  scorecard.md               # CI-automatable migration progress tracker
  template/
    .github/
      copilot-instructions.md            # Repo-level Copilot instructions
      instructions/
        core-domain.instructions.md      # Rules for core domain code
        adapters.instructions.md         # Rules for adapter modules
        autoconfig.instructions.md       # Rules for auto-configuration
        angular.instructions.md          # Rules for Angular frontend code
        tests.instructions.md            # Rules for all test code
    src/
      main/java/
        MarkerClass.java.template        # @Modulithic marker class
      test/java/
        ModulithStructureTest.java.template   # Modulith verification test
        ArchitectureRulesTest.java.template   # Custom ArchUnit rules
    docker/
      docker-compose.yml.template        # Docker build containers
    Makefile.template                    # Docker-only build scaffolding
```

## How to Use

### 1. Copy templates into your service repository

```bash
# From the service repo root
cp -r /path/to/onefinancial/migration/template/.github .
cp /path/to/onefinancial/migration/template/Makefile.template Makefile
cp /path/to/onefinancial/migration/template/docker/docker-compose.yml.template docker/docker-compose.yml
```

### 2. Replace placeholders

Every template uses `<PLACEHOLDER>` format. Search and replace:

| Placeholder | Example | Description |
|-------------|---------|-------------|
| `<SERVICE_NAME>` | `billing-service` | Lowercase kebab-case service name |
| `<SERVICE_NAME_PASCAL>` | `BillingService` | PascalCase service name |
| `<BASE_PACKAGE>` | `com.onefinancial.billing` | Root Java package |
| `<MARKER_CLASS>` | `BillingServiceModule` | Spring Modulith marker class name |
| `<DB_PREFIX>` | `bs_` | Database table prefix (2-3 chars + underscore) |
| `<PROPERTY_PREFIX>` | `billing.service` | Spring property namespace |
| `<ANGULAR_PREFIX>` | `bsui` | Angular selector prefix |
| `<DOCKER_IMAGE_JAVA>` | `eclipse-temurin:21-jdk-alpine` | Java build image |
| `<DOCKER_IMAGE_NODE>` | `node:20-alpine` | Node build image |

### 3. Copy Java templates

```bash
# Adjust package path to match <BASE_PACKAGE>
mkdir -p src/main/java/com/onefinancial/billing
mkdir -p src/test/java/com/onefinancial/billing

cp migration/template/src/main/java/MarkerClass.java.template \
   src/main/java/com/onefinancial/billing/BillingServiceModule.java

cp migration/template/src/test/java/ModulithStructureTest.java.template \
   src/test/java/com/onefinancial/billing/ModulithStructureTest.java

cp migration/template/src/test/java/ArchitectureRulesTest.java.template \
   src/test/java/com/onefinancial/billing/ArchitectureRulesTest.java
```

### 4. Follow the phased migration workflow

The recommended migration order matches the scorecard phases:

1. **Phase 1 -- Foundation**: Package structure, marker class, Modulith test, CI pipeline
2. **Phase 2 -- Core Domain**: Extract domain model, ports, events into `core/` package
3. **Phase 3 -- Adapters**: Create persistence, REST, events adapters with bridge configs
4. **Phase 4 -- Auto-configuration**: Wire everything with `@ConditionalOnProperty` gates
5. **Phase 5 -- Frontend**: Migrate Angular components to library pattern (if applicable)

Each phase has a gate in the scorecard (`migration/scorecard.md`) that CI can verify
before proceeding to the next phase.

### 5. Track progress

Use the scorecard to track migration progress. The scorecard is designed to be
computed automatically by CI -- see `scorecard.md` for verification commands.

## Incremental Adoption

You do NOT need to adopt all instruction files at once. The recommended order:

1. **Start with repo-level only**: Copy `.github/copilot-instructions.md` first.
   This gives Copilot baseline context for all files in the repo.

2. **Add path-specific as you migrate each layer**:
   - Phase 1-2: Add `core-domain.instructions.md` and `tests.instructions.md`
   - Phase 3: Add `adapters.instructions.md`
   - Phase 4: Add `autoconfig.instructions.md`
   - Phase 5: Add `angular.instructions.md` (if applicable)

Adding instructions incrementally avoids overwhelming Copilot with rules for
code patterns that do not yet exist in the repo.

## Single Source of Truth

The instruction files in this template are derived from the project's `CLAUDE.md`
and the patterns in `customer-registry-starter`. To keep them in sync, consider
generating the instruction files from a single source:

```bash
# Concept: generate path-specific instructions from CLAUDE.md sections
# This avoids drift between the canonical rules and Copilot instructions.
#
# For now, manually compare with CLAUDE.md when updating.
# A future generate-instructions.sh script could automate this.
```

When the canonical architecture rules change in `CLAUDE.md`, update the
corresponding instruction file(s) here and re-distribute to migrating services.

## Complexity Tiers

Each instruction file includes guidance for services at different complexity
levels. Not every service needs every pattern:

| Tier | Characteristics | What to Skip |
|------|-----------------|-------------|
| **Simple** | Single entity, no UI, 1-2 adapters | Angular instructions, observability, migration adapter |
| **Standard** | Multiple entities, REST + persistence | Some SPI patterns, advanced event choreography |
| **Advanced** | Full lifecycle, multi-tenant, JSONB | Use everything -- this is the reference level |

Start with the patterns you need today and adopt more as complexity grows.

## Copilot Chat Migration Workflow

For a guided, prompt-driven migration workflow using GitHub Copilot Chat, see:

| Document | Purpose |
|----------|---------|
| [Copilot Migration Strategy](../docs/copilot-migration-strategy.md) | Step-by-step process guide with workspace setup, phased execution, and troubleshooting |
| [Copilot Prompts Library](copilot-prompts.md) | Ready-to-paste prompts for each migration phase (0-5), with expected outputs and recovery |
| [Migration Workflow Instructions](template/.github/instructions/migration-workflow.instructions.md) | Instruction file Copilot Chat reads automatically via `.github/instructions/` |
| [Lessons Learned Template](template/MIGRATION-LESSONS.md.template) | Post-migration feedback template for continuous improvement |

The workflow uses a multi-root VS Code workspace with three directories (`reference/`,
`legacy/`, `target/`) and follows the "read reference -> analyze legacy -> generate code"
pattern to ensure Copilot adheres to architectural rules.

## Reference Implementation

The `customer-registry-starter` module in this repository is the reference
implementation (Advanced tier). When in doubt, consult its source code for
working examples of every pattern described in the instruction files.
