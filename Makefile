.PHONY: build test verify clean build-java build-angular test-java test-java-unit test-java-integration test-angular up down install-angular update-presentation migration-verify migration-verify-full migration-status migration-guide migration-init migration-record test-migration test-presentation

DOCKER_COMPOSE = docker compose -f docker/docker-compose.yml
JAVA_RUN = $(DOCKER_COMPOSE) run --rm --no-deps java-build
JAVA_RUN_DB = $(DOCKER_COMPOSE) run --rm java-build
JAVA_RUN_TC = $(DOCKER_COMPOSE) run --rm java-build-testcontainers
NODE_RUN = $(DOCKER_COMPOSE) run --rm node-build
PYTHON_RUN = $(DOCKER_COMPOSE) run --rm python-build
PYYAML_VERSION = 6.0.2

# ─── Full Build ───────────────────────────────────────────────

build: build-java build-angular

test: test-java test-angular

verify: verify-java test-angular

clean: clean-java clean-angular

# ─── Java ─────────────────────────────────────────────────────

build-java:
	$(JAVA_RUN) mvn clean package -DskipTests

test-java:
	$(JAVA_RUN_DB) mvn test

test-java-unit:
	$(JAVA_RUN) mvn test -Dtest='!*IntegrationTest' -Dsurefire.failIfNoSpecifiedTests=false

test-java-integration:
	$(JAVA_RUN_TC) mvn test -Dtest='*IntegrationTest' -Dsurefire.failIfNoSpecifiedTests=false

verify-java:
	$(JAVA_RUN_DB) mvn clean verify

clean-java:
	$(JAVA_RUN) mvn clean

# ─── Angular ──────────────────────────────────────────────────

install-angular:
	$(NODE_RUN) npm ci

build-angular:
	$(NODE_RUN) npm run build

test-angular:
	$(NODE_RUN) npx jest --config projects/customer-registry-ui/jest.config.ts --no-cache

lint-angular:
	$(NODE_RUN) npm run lint

clean-angular:
	$(NODE_RUN) sh -c "rm -rf dist .angular"

# ─── Infrastructure ──────────────────────────────────────────

up:
	$(DOCKER_COMPOSE) up -d postgres pgadmin

down:
	$(DOCKER_COMPOSE) down

# ─── Security ────────────────────────────────────────────────
# Placeholder: requires Maven security profile to be configured in pom.xml.

security-scan:
	@echo "Security scanning is run via Azure Pipelines (see .azure/pipelines/templates/security-scan.yml)."
	@echo "To run locally, configure the -Psecurity Maven profile in the root pom.xml."

# ─── Presentation ───────────────────────────────────────────

update-presentation:
	$(PYTHON_RUN) sh -c "pip install --quiet pyyaml==$(PYYAML_VERSION) && python3 scripts/update_presentation.py"

# ─── Migration CLI ──────────────────────────────────────────────────────

migration-verify:
	$(PYTHON_RUN) sh -c "pip install --quiet pyyaml==$(PYYAML_VERSION) && python3 scripts/migration_cli.py verify --self-test $(ARGS)"

migration-verify-full: migration-verify
	@echo "── Full: Docker-based architecture tests ──"
	$(JAVA_RUN) mvn test -Dtest='ModulithStructureTest' -Dsurefire.failIfNoSpecifiedTests=false -pl customer-registry-starter
	$(JAVA_RUN) mvn test -Dtest='ArchitectureRulesTest' -Dsurefire.failIfNoSpecifiedTests=false -pl customer-registry-starter

migration-status:
	$(PYTHON_RUN) sh -c "pip install --quiet pyyaml==$(PYYAML_VERSION) && python3 scripts/migration_cli.py status $(ARGS)"

migration-guide:
	$(PYTHON_RUN) sh -c "pip install --quiet pyyaml==$(PYYAML_VERSION) && python3 scripts/migration_cli.py guide $(ARGS)"

migration-init:
	$(PYTHON_RUN) sh -c "pip install --quiet pyyaml==$(PYYAML_VERSION) && python3 scripts/migration_cli.py init $(ARGS)"

migration-record:
	$(PYTHON_RUN) sh -c "pip install --quiet pyyaml==$(PYYAML_VERSION) && python3 scripts/migration_cli.py record $(ARGS)"

test-migration:
	$(PYTHON_RUN) sh -c "pip install --quiet pyyaml==$(PYYAML_VERSION) pytest && python3 -m pytest scripts/test_migration_cli.py -v"

test-presentation:
	$(PYTHON_RUN) sh -c "pip install --quiet pyyaml==$(PYYAML_VERSION) pytest && python3 -m pytest scripts/test_update_presentation.py -v"
