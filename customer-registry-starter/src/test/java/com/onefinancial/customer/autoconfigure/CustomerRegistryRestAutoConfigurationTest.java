package com.onefinancial.customer.autoconfigure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerRegistryRestAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(
            CustomerRegistryCoreAutoConfiguration.class,
            CustomerRegistryRestAutoConfiguration.class,
            WebMvcAutoConfiguration.class
        ));

    @Test
    @DisplayName("should not register REST beans when disabled")
    void noBeansWhenDisabled() {
        contextRunner
            .withPropertyValues("customer.registry.enabled=true")
            .run(context ->
                assertThat(context).doesNotHaveBean("customerController"));
    }

    @Test
    @DisplayName("should register REST beans when enabled")
    void registerBeansWhenEnabled() {
        contextRunner
            .withPropertyValues(
                "customer.registry.enabled=true",
                "customer.registry.features.rest-api=true")
            .run(context ->
                assertThat(context).hasBean("customerController"));
    }

    @Test
    @DisplayName("should not register REST beans when registry itself is disabled")
    void noBeansWhenRegistryDisabled() {
        contextRunner
            .withPropertyValues(
                "customer.registry.enabled=false",
                "customer.registry.features.rest-api=true")
            .run(context ->
                assertThat(context).doesNotHaveBean("customerController"));
    }
}
