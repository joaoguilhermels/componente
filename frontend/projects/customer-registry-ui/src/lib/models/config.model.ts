/**
 * Configuration for the Customer Registry UI library.
 * Provided via the CUSTOMER_REGISTRY_UI_CONFIG injection token.
 */
export interface CustomerRegistryUiConfig {
  /** Base URL for the customer registry REST API (default: '/api/v1') */
  apiBaseUrl: string;
  /** Default locale for i18n (default: 'pt-BR') */
  locale: string;
  /** Feature flags */
  features: CustomerRegistryUiFeatures;
}

export interface CustomerRegistryUiFeatures {
  /** Enable search component (default: true) */
  search: boolean;
  /** Enable inline editing in list (default: false) */
  inlineEdit: boolean;
  /** Enable address management (default: true) */
  addresses: boolean;
  /** Enable contact management (default: true) */
  contacts: boolean;
}

/** Default configuration values */
export const DEFAULT_CONFIG: CustomerRegistryUiConfig = {
  apiBaseUrl: '/api/v1',
  locale: 'pt-BR',
  features: {
    search: true,
    inlineEdit: false,
    addresses: true,
    contacts: true,
  },
};
