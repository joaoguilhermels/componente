import {
  EnvironmentProviders,
  makeEnvironmentProviders,
  Provider,
} from '@angular/core';
import {
  CustomerRegistryUiConfig,
  CustomerRegistryUiFeatures,
  DEFAULT_CONFIG,
} from './models/config.model';
import {
  CustomerValidationRule,
  ExtraFieldDefinition,
  FieldRendererRegistration,
  RendererErrorReporter,
} from './models/extensibility.model';
import {
  CUSTOMER_EXTRA_FIELDS,
  CUSTOMER_FIELD_RENDERERS,
  CUSTOMER_I18N_OVERRIDES,
  CUSTOMER_REGISTRY_UI_CONFIG,
  CUSTOMER_UI_RENDERER_ERROR_REPORTER,
  CUSTOMER_VALIDATION_RULES,
} from './tokens';

/**
 * Partial config that allows partial features (deep-merged with defaults).
 */
export type PartialCustomerRegistryUiConfig = Partial<
  Omit<CustomerRegistryUiConfig, 'features'>
> & {
  features?: Partial<CustomerRegistryUiFeatures>;
};

/**
 * Options accepted by provideCustomerRegistry().
 * All fields are optional — defaults are merged from DEFAULT_CONFIG.
 */
export interface CustomerRegistryProviderOptions {
  /** Partial config overrides (deep-merged with defaults) */
  config?: PartialCustomerRegistryUiConfig;
  /** Extra fields to inject into the customer form */
  extraFields?: ExtraFieldDefinition[];
  /** Additional validation rules */
  validationRules?: CustomerValidationRule[];
  /** Custom field renderers */
  fieldRenderers?: FieldRendererRegistration[];
  /** i18n string overrides keyed by locale */
  i18nOverrides?: Record<string, Record<string, string>>;
  /** Custom error reporter for renderer failures */
  errorReporter?: RendererErrorReporter;
}

/**
 * Convenience function to configure the Customer Registry UI library.
 *
 * Usage in app.config.ts:
 * ```typescript
 * export const appConfig: ApplicationConfig = {
 *   providers: [
 *     provideCustomerRegistry({
 *       config: { apiBaseUrl: '/my-api/v1' },
 *       extraFields: [{ key: 'loyalty', labelKey: 'field.loyalty', type: 'text' }],
 *     }),
 *   ],
 * };
 * ```
 */
export function provideCustomerRegistry(
  options: CustomerRegistryProviderOptions = {}
): EnvironmentProviders {
  const mergedConfig: CustomerRegistryUiConfig = {
    ...DEFAULT_CONFIG,
    ...options.config,
    features: {
      ...DEFAULT_CONFIG.features,
      ...options.config?.features,
    },
  };

  const providers: Provider[] = [
    { provide: CUSTOMER_REGISTRY_UI_CONFIG, useValue: mergedConfig },
  ];

  if (options.extraFields) {
    providers.push({
      provide: CUSTOMER_EXTRA_FIELDS,
      useValue: options.extraFields,
      multi: true,
    });
  }

  if (options.validationRules) {
    providers.push({
      provide: CUSTOMER_VALIDATION_RULES,
      useValue: options.validationRules,
      multi: true,
    });
  }

  if (options.fieldRenderers) {
    providers.push({
      provide: CUSTOMER_FIELD_RENDERERS,
      useValue: options.fieldRenderers,
      multi: true,
    });
  }

  if (options.i18nOverrides) {
    providers.push({
      provide: CUSTOMER_I18N_OVERRIDES,
      useValue: options.i18nOverrides,
    });
  }

  if (options.errorReporter) {
    providers.push({
      provide: CUSTOMER_UI_RENDERER_ERROR_REPORTER,
      useValue: options.errorReporter,
    });
  }

  return makeEnvironmentProviders(providers);
}
