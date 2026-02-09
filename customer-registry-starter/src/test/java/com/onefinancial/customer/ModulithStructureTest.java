package com.onefinancial.customer;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import static org.assertj.core.api.Assertions.assertThat;

class ModulithStructureTest {

    @Test
    void verifyModuleStructure() {
        ApplicationModules modules = ApplicationModules.of(CustomerRegistryModule.class);
        modules.verify();
    }

    @Test
    void shouldHaveAtLeastOneModule() {
        ApplicationModules modules = ApplicationModules.of(CustomerRegistryModule.class);

        assertThat(modules).isNotEmpty();
        modules.forEach(System.out::println);
    }
}
