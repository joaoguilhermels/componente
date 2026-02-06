import { InjectionToken } from '@angular/core';
import {
  CustomerRegistryUiConfig,
  DEFAULT_CONFIG,
} from '../models/config.model';
import {
  CustomerValidationRule,
  ExtraFieldDefinition,
  FieldRendererRegistration,
  RendererErrorReporter,
} from '../models/extensibility.model';

/**
 * Main configuration token for the library.
 * Provide via provideCustomerRegistry() or directly in your app config.
 */
export const CUSTOMER_REGISTRY_UI_CONFIG =
  new InjectionToken<CustomerRegistryUiConfig>(
    'CUSTOMER_REGISTRY_UI_CONFIG',
    { providedIn: 'root', factory: () => DEFAULT_CONFIG }
  );

/**
 * Multi-token: host apps add extra fields to the customer form.
 * Each provider contributes an array of ExtraFieldDefinition.
 */
export const CUSTOMER_EXTRA_FIELDS =
  new InjectionToken<ExtraFieldDefinition[][]>(
    'CUSTOMER_EXTRA_FIELDS'
  );

/**
 * Multi-token: host apps inject additional validation rules.
 * Each provider contributes an array of CustomerValidationRule.
 */
export const CUSTOMER_VALIDATION_RULES =
  new InjectionToken<CustomerValidationRule[][]>(
    'CUSTOMER_VALIDATION_RULES'
  );

/**
 * Multi-token: host apps register custom field renderers.
 * Each provider contributes an array of FieldRendererRegistration.
 */
export const CUSTOMER_FIELD_RENDERERS =
  new InjectionToken<FieldRendererRegistration[][]>(
    'CUSTOMER_FIELD_RENDERERS'
  );

/**
 * Override token: host apps can supply i18n string overrides.
 * Keys are locale codes; values are key-value translation maps.
 */
export const CUSTOMER_I18N_OVERRIDES =
  new InjectionToken<Record<string, Record<string, string>>>(
    'CUSTOMER_I18N_OVERRIDES',
    { providedIn: 'root', factory: () => ({}) }
  );

/**
 * Override token: host apps supply a custom error reporter
 * for graceful degradation of custom field renderers.
 */
export const CUSTOMER_UI_RENDERER_ERROR_REPORTER =
  new InjectionToken<RendererErrorReporter>(
    'CUSTOMER_UI_RENDERER_ERROR_REPORTER',
    {
      providedIn: 'root',
      factory: () => ({
        report: (rendererId: string, error: unknown) => {
          console.error(
            `[CustomerRegistryUI] Renderer "${rendererId}" failed:`,
            error
          );
        },
      }),
    }
  );
