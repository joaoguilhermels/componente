# Migration Scorecard

> This scorecard should be computed automatically by CI (`migration-check.yml`),
> not manually maintained. Each dimension includes a CI verification command.

## Scoring Table

| Dimension | Weight | Gate Phase | How to Verify (CI) |
|-----------|--------|------------|--------------------|
| Package structure (hexagonal layout) | 10% | Phase 1 | `test -d src/main/java/**/core && test -d src/main/java/**/persistence && test -d src/main/java/**/rest` |
| Modulith marker + test passes | 10% | Phase 1 | `mvn test -Dtest=ModulithStructureTest` |
| Core isolation (zero infra deps) | 15% | Phase 2 | `mvn test -Dtest=ArchitectureRulesTest#corePackageMustNotDependOnInfrastructure` |
| Port interfaces for all external access | 10% | Phase 2 | `find src/main/java -path '*/core/port/*.java' -type f \| wc -l` (expect >= 1) |
| Bridge config on all adapters | 5% | Phase 3 | `grep -rl 'BridgeConfiguration' src/main/java --include='*.java' \| wc -l` (expect >= 1 per adapter) |
| `@ConditionalOnMissingBean` on all beans | 5% | Phase 4 | `grep -c '@Bean' src/main/java/**/autoconfigure/*.java` == `grep -c '@ConditionalOnMissingBean' src/main/java/**/autoconfigure/*.java` |
| Feature flags off by default | 5% | Phase 4 | `grep -c 'havingValue = "true"' src/main/java/**/autoconfigure/*.java` (all properties require explicit opt-in) |
| Auto-config in META-INF | 5% | Phase 4 | `test -f src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` |
| Domain unit tests >= 80% coverage | 10% | Phase 2 | JaCoCo report: `xmllint --xpath '//package[@name="core"]/counter[@type="LINE"]/@covered' target/site/jacoco/jacoco.xml` |
| Adapter integration tests | 10% | Phase 3 | `find src/test/java -name '*IntegrationTest.java' \| wc -l` (expect >= 1 per adapter) |
| Auto-config context runner tests | 5% | Phase 4 | `grep -rl 'ApplicationContextRunner' src/test/java --include='*.java' \| wc -l` (expect >= 1) |
| Angular patterns (standalone + OnPush) | 5% | Phase 5 | `grep -c 'standalone: true' frontend/src/**/*.ts` and `grep -c 'OnPush' frontend/src/**/*.ts` |
| CI pipeline with architecture gates | 5% | Phase 1 | `test -f .github/workflows/ci.yml \|\| test -f .azure/pipelines/ci.yml` |

**Total: 100%**

## Phase Gates

A service must pass all dimensions in a phase before moving to the next:

### Phase 1 -- Foundation (25%)
- [ ] Package structure exists (10%)
- [ ] Modulith marker + test passes (10%)
- [ ] CI pipeline with architecture gates (5%)

### Phase 2 -- Core Domain (35%)
- [ ] Core isolation verified by ArchUnit (15%)
- [ ] Port interfaces defined (10%)
- [ ] Domain unit test coverage >= 80% (10%)

### Phase 3 -- Adapters (15%)
- [ ] Bridge configs on all adapters (5%)
- [ ] Integration tests for each adapter (10%)

### Phase 4 -- Auto-configuration (20%)
- [ ] `@ConditionalOnMissingBean` coverage (5%)
- [ ] Feature flags default to off (5%)
- [ ] Auto-config registered in META-INF (5%)
- [ ] Context runner tests (5%)

### Phase 5 -- Frontend (5%)
- [ ] Angular standalone components with OnPush (5%)

## CI Integration

Add a `migration-check` job to your CI pipeline that computes the score:

```yaml
# .github/workflows/migration-check.yml
name: Migration Score
on: [pull_request]
jobs:
  score:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Phase 1 - Package structure
        run: |
          CORE=$(find src/main/java -type d -name "core" | head -1)
          test -n "$CORE" && echo "PASS: core/ exists" || echo "FAIL: core/ missing"
      - name: Phase 1 - Modulith test
        run: make test-java-unit TEST=ModulithStructureTest
      - name: Phase 2 - Core isolation
        run: make test-java-unit TEST=ArchitectureRulesTest
      - name: Phase 2 - Port interfaces
        run: |
          COUNT=$(find src/main/java -path '*/core/port/*.java' | wc -l)
          test "$COUNT" -ge 1 && echo "PASS: $COUNT port(s)" || echo "FAIL: no ports"
      - name: Phase 4 - Auto-config META-INF
        run: |
          FILE="src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports"
          test -f "$FILE" && echo "PASS" || echo "FAIL: $FILE missing"
      - name: Phase 4 - ConditionalOnMissingBean parity
        run: |
          BEANS=$(grep -r '@Bean' src/main/java/**/autoconfigure/ --include='*.java' | wc -l)
          CONDS=$(grep -r '@ConditionalOnMissingBean' src/main/java/**/autoconfigure/ --include='*.java' | wc -l)
          test "$BEANS" -eq "$CONDS" && echo "PASS: $BEANS/$CONDS" || echo "FAIL: $BEANS beans vs $CONDS conditionals"
```

## Scoring Formula

```
score = sum(dimension_weight * (1 if passing else 0))
```

A service is considered fully migrated when `score >= 95%` (the 5% Angular phase
is optional for backend-only services).
