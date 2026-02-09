package com.onefinancial.customer.autoconfigure;

import com.onefinancial.customer.rest.CustomerRestConfiguration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for the Customer Registry REST API.
 *
 * <p>Gated by {@code customer.registry.enabled=true} and
 * {@code customer.registry.features.rest-api=true}.
 * Also requires Spring Web on the classpath.</p>
 */
@AutoConfiguration(after = CustomerRegistryCoreAutoConfiguration.class)
@ConditionalOnProperty(
    prefix = "customer.registry",
    name = {"enabled", "features.rest-api"},
    havingValue = "true"
)
@ConditionalOnClass(name = "org.springframework.web.servlet.DispatcherServlet")
@Import(CustomerRestConfiguration.class)
public class CustomerRegistryRestAutoConfiguration {
}
