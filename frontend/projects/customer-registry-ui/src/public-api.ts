/*
 * Public API Surface of @onefinancial/customer-registry-ui
 *
 * Export all public types, components, services, tokens, and utilities here.
 * This barrel file is the single entry point for library consumers.
 *
 * Note: `export *` re-exports make all types from sub-barrel files part of
 * the public API. If any internal types should be hidden from consumers,
 * replace the wildcard re-export with explicit named exports for that module.
 */

export const CUSTOMER_REGISTRY_UI_VERSION = '0.1.0';

// Models
export * from './lib/models';

// Injection Tokens
export * from './lib/tokens';

// Provider function
export * from './lib/provide-customer-registry';

// i18n
export * from './lib/i18n';

// Services
export * from './lib/services';

// Validators
export * from './lib/validators';

// Components
export * from './lib/components';
