package com.oneff.customer;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModulithStructureTest {

    @Test
    void verifyModuleStructure() {
        ApplicationModules modules = ApplicationModules.of(CustomerRegistryModule.class);
        modules.verify();
    }

    @Test
    void printModuleArrangement() {
        ApplicationModules modules = ApplicationModules.of(CustomerRegistryModule.class);
        modules.forEach(System.out::println);
    }
}
