---
applyTo: "src/test/**/*.java,**/*.spec.ts"
---

# Testing Rules

All code follows TDD: red-green-refactor. Write the test FIRST, see it fail,
then implement the production code.

## Java -- Architectural Tests

### ModulithStructureTest (THE gate)

This test verifies the module structure. It must always pass before any PR
merges:

```java
class ModulithStructureTest {
    @Test
    void verifyModuleStructure() {
        ApplicationModules modules = ApplicationModules.of(<MARKER_CLASS>.class);
        modules.verify();
    }
}
```

### ArchitectureRulesTest (Custom rules)

Custom ArchUnit rules that go beyond what `modules.verify()` catches:

- `corePackageMustNotDependOnInfrastructure`
- `autoConfigMustNotUseComponentScan`
- `eventsMustBeRecords`
- `controllersMustBePackagePrivate`
- `bridgeConfigsMustBePublic`

## Java -- Auto-Config Tests

Use `ApplicationContextRunner` to verify beans are (not) registered:

```java
@Test
void shouldNotRegisterBeansWhenDisabled() {
    new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(CoreAutoConfiguration.class))
        .run(context -> assertThat(context).doesNotHaveBean(CustomerService.class));
}

@Test
void shouldRegisterBeansWhenEnabled() {
    new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(CoreAutoConfiguration.class))
        .withPropertyValues("<PROPERTY_PREFIX>.enabled=true")
        .run(context -> assertThat(context).hasSingleBean(CustomerService.class));
}
```

## Java -- Integration Tests

- Use `*IntegrationTest` naming convention for selective execution
- Testcontainers for database tests (Docker socket mount required)
- `@DataJpaTest` includes Liquibase -- disable with property:
  `spring.liquibase.enabled=false`
- `@WebMvcTest` in library needs `@SpringBootApplication` inner class

```java
@DataJpaTest
@TestPropertySource(properties = "spring.liquibase.enabled=false")
class CustomerRepositoryIntegrationTest {
    // ...
}
```

## Angular -- Jest Configuration

- Use `jest-preset-angular@14.6.2` for Angular 17 compatibility
- Config key is `setupFilesAfterEnv` (NOT `setupFilesAfterSetup`)
- Zone setup: `setupZoneTestEnv()` from `jest-preset-angular/setup-env/zone`
- `ts-node` required as devDependency for Jest to parse `.ts` config

## Angular -- Component Tests

Test standalone components with the testing module:

```typescript
describe('CustomerListComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CustomerListComponent],
      providers: [
        { provide: CustomerStateService, useValue: mockStateService },
      ],
    }).compileComponents();
  });
});
```

## Test Naming

- Java: descriptive method names (`shouldRejectNullDocument`, `shouldFindByDocument`)
- Angular: `describe` blocks mirror component name, `it` blocks describe behavior
