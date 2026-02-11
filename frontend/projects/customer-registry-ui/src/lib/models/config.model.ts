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
  /**
   * Optional callback invoked when a translation key is not found.
   * Return a string to use as the translation, or undefined to fall back to the raw key.
   */
  onMissingKey?: (key: string, locale: string) => string | undefined;
}

export interface CustomerRegistryUiFeatures {
  /** Enable search component (default: true) */
  search: boolean;
  /** Reserved for future use. Not currently implemented. */
  inlineEdit: boolean;
  /** Controls address display in detail view. Form editing is not yet supported. */
  addresses: boolean;
  /** Controls contact display in detail view. Form editing is not yet supported. */
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
