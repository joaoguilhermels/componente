package com.example.crm;

import com.oneff.customer.core.exception.CustomerValidationException;
import com.oneff.customer.core.model.AttributeValue.StringValue;
import com.oneff.customer.core.model.Customer;
import com.oneff.customer.core.model.CustomerType;
import com.oneff.customer.core.port.CustomerEventPublisher;
import com.oneff.customer.core.port.CustomerRepository;
import com.oneff.customer.core.service.CustomerRegistryService;
import com.oneff.customer.core.spi.CustomerEnricher;
import com.oneff.customer.core.spi.CustomerValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class ExampleCrmApplicationTest {

    @Autowired
    CustomerRegistryService service;

    @Autowired
    CustomerRepository repository;

    @Autowired
    CustomerEventPublisher eventPublisher;

    @Autowired
    List<CustomerValidator> validators;

    @Autowired
    List<CustomerEnricher> enrichers;

    @Test
    @DisplayName("application context should load with all beans")
    void contextLoads() {
        assertThat(service).isNotNull();
        assertThat(repository).isNotNull();
        assertThat(eventPublisher).isNotNull();
    }

    @Nested
    @DisplayName("Custom extensions")
    class CustomExtensions {

        @Test
        @DisplayName("should register LoyaltyNumberValidator")
        void loyaltyValidatorRegistered() {
            assertThat(validators)
                .extracting(v -> v.getClass().getSimpleName())
                .contains("LoyaltyNumberValidator");
        }

        @Test
        @DisplayName("should register LoyaltyEnricher")
        void loyaltyEnricherRegistered() {
            assertThat(enrichers)
                .extracting(e -> e.getClass().getSimpleName())
                .contains("LoyaltyEnricher");
        }

        @Test
        @DisplayName("LoyaltyNumberValidator should reject short PJ names")
        void rejectShortPjName() {
            Customer pj = Customer.createPJ("11222333000181", "AB");
            assertThatThrownBy(() -> service.register(pj))
                .isInstanceOf(CustomerValidationException.class);
        }

        @Test
        @DisplayName("LoyaltyNumberValidator should accept valid PJ names")
        void acceptValidPjName() {
            Customer pj = Customer.createPJ("11222333000181", "Empresa XYZ Ltda");
            Customer saved = service.register(pj);
            assertThat(saved).isNotNull();
            assertThat(saved.getDisplayName()).isEqualTo("Empresa XYZ Ltda");
        }

        @Test
        @DisplayName("LoyaltyEnricher should add default loyalty tier")
        void enrichWithLoyaltyTier() {
            Customer pf = Customer.createPF("52998224725", "Maria Silva");
            Customer saved = service.register(pf);

            StringValue tier = saved.getAttributes().get("loyaltyTier", StringValue.class);
            assertThat(tier).isNotNull();
            assertThat(tier.value()).isEqualTo("BRONZE");
        }
    }
}
