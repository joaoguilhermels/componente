package com.oneff.customer.autoconfigure;

import com.oneff.customer.core.port.CustomerEventPublisher;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerRegistryEventsAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(
            CustomerRegistryCoreAutoConfiguration.class,
            CustomerRegistryEventsAutoConfiguration.class
        ));

    @Test
    @DisplayName("should use NoOp publisher when events disabled")
    void noOpWhenDisabled() {
        contextRunner
            .withPropertyValues("customer.registry.enabled=true")
            .run(context -> {
                CustomerEventPublisher publisher =
                    context.getBean(CustomerEventPublisher.class);
                assertThat(publisher).isInstanceOf(NoOpEventPublisher.class);
            });
    }

    @Test
    @DisplayName("should use Spring adapter when events enabled")
    void springAdapterWhenEnabled() {
        contextRunner
            .withPropertyValues(
                "customer.registry.enabled=true",
                "customer.registry.features.publish-events=true")
            .run(context -> {
                CustomerEventPublisher publisher =
                    context.getBean(CustomerEventPublisher.class);
                // Verify it's NOT the NoOp fallback (it's the Spring adapter)
                assertThat(publisher).isNotInstanceOf(NoOpEventPublisher.class);
            });
    }

    @Test
    @DisplayName("should not register events when registry disabled")
    void nothingWhenRegistryDisabled() {
        contextRunner
            .withPropertyValues(
                "customer.registry.enabled=false",
                "customer.registry.features.publish-events=true")
            .run(context ->
                assertThat(context).doesNotHaveBean(CustomerEventPublisher.class));
    }
}
