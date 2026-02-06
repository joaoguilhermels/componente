.PHONY: build test verify clean build-java build-angular test-java test-angular up down

DOCKER_COMPOSE = docker compose -f docker/docker-compose.yml
JAVA_RUN = $(DOCKER_COMPOSE) run --rm java-build
NODE_RUN = $(DOCKER_COMPOSE) run --rm node-build

# ─── Full Build ───────────────────────────────────────────────

build: build-java build-angular

test: test-java test-angular

verify: verify-java test-angular

clean: clean-java clean-angular

# ─── Java ─────────────────────────────────────────────────────

build-java:
	$(JAVA_RUN) mvn clean package -DskipTests

test-java:
	$(JAVA_RUN) mvn test

verify-java:
	$(JAVA_RUN) mvn clean verify

clean-java:
	$(JAVA_RUN) mvn clean

# ─── Angular ──────────────────────────────────────────────────

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

# ─── Security (future) ───────────────────────────────────────

security-scan:
	$(JAVA_RUN) mvn verify -Psecurity
