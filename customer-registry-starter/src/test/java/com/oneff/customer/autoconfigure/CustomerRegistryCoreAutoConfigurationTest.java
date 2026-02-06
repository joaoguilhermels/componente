package com.oneff.customer.autoconfigure;

import com.oneff.customer.core.model.Customer;
import com.oneff.customer.core.port.CustomerEventPublisher;
import com.oneff.customer.core.port.CustomerRepository;
import com.oneff.customer.core.service.CustomerRegistryService;
import com.oneff.customer.core.spi.CustomerValidator;
import com.oneff.customer.core.spi.ValidationResult;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerRegistryCoreAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(CustomerRegistryCoreAutoConfiguration.class));

    @Nested
    class WhenDisabled {

        @Test
        void shouldRegisterNoBeans() {
            contextRunner
                .run(context -> {
                    assertThat(context).doesNotHaveBean(CustomerRegistryService.class);
                    assertThat(context).doesNotHaveBean(CustomerRepository.class);
                    assertThat(context).doesNotHaveBean(CustomerEventPublisher.class);
                });
        }

        @Test
        void shouldRegisterNoBeansWhenEnabledIsFalse() {
            contextRunner
                .withPropertyValues("customer.registry.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(CustomerRegistryService.class);
                });
        }
    }

    @Nested
    class WhenEnabled {

        @Test
        void shouldRegisterCoreBeans() {
            contextRunner
                .withPropertyValues("customer.registry.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(CustomerRegistryService.class);
                    assertThat(context).hasSingleBean(CustomerRepository.class);
                    assertThat(context).hasSingleBean(CustomerEventPublisher.class);
                    assertThat(context).hasBean("customerRegistryMessageSource");
                });
        }

        @Test
        void shouldUseFallbackInMemoryRepository() {
            contextRunner
                .withPropertyValues("customer.registry.enabled=true")
                .run(context -> {
                    CustomerRepository repo = context.getBean(CustomerRepository.class);
                    assertThat(repo).isInstanceOf(InMemoryCustomerRepository.class);
                });
        }

        @Test
        void shouldUseFallbackNoOpPublisher() {
            contextRunner
                .withPropertyValues("customer.registry.enabled=true")
                .run(context -> {
                    CustomerEventPublisher publisher =
                        context.getBean(CustomerEventPublisher.class);
                    assertThat(publisher).isInstanceOf(NoOpEventPublisher.class);
                });
        }

        @Test
        void shouldRegisterScopedMessageSource() {
            contextRunner
                .withPropertyValues("customer.registry.enabled=true")
                .run(context -> {
                    MessageSource messageSource =
                        context.getBean("customerRegistryMessageSource", MessageSource.class);
                    assertThat(messageSource).isNotNull();
                });
        }
    }

    @Nested
    class ConsumerOverrides {

        @Test
        void shouldAllowConsumerToOverrideRepository() {
            contextRunner
                .withPropertyValues("customer.registry.enabled=true")
                .withUserConfiguration(CustomRepositoryConfig.class)
                .run(context -> {
                    CustomerRepository repo = context.getBean(CustomerRepository.class);
                    assertThat(repo).isInstanceOf(StubCustomerRepository.class);
                });
        }

        @Test
        void shouldPickUpConsumerValidators() {
            contextRunner
                .withPropertyValues("customer.registry.enabled=true")
                .withUserConfiguration(CustomValidatorConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(CustomerRegistryService.class);
                    assertThat(context.getBeansOfType(CustomerValidator.class)).hasSize(1);
                });
        }

        @Configuration
        static class CustomRepositoryConfig {
            @Bean
            CustomerRepository customerRepository() {
                return new StubCustomerRepository();
            }
        }

        /**
         * A distinct repository class to prove that consumer beans
         * override the starter's default InMemoryCustomerRepository.
         */
        static class StubCustomerRepository extends InMemoryCustomerRepository {
        }

        @Configuration
        static class CustomValidatorConfig {
            @Bean
            CustomerValidator loyaltyValidator() {
                return customer -> ValidationResult.valid();
            }
        }
    }
}
