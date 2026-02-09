import { TestBed } from '@angular/core/testing';
import { provideCustomerRegistry } from './provide-customer-registry';
import {
  CUSTOMER_EXTRA_FIELDS,
  CUSTOMER_FIELD_RENDERERS,
  CUSTOMER_I18N_OVERRIDES,
  CUSTOMER_REGISTRY_UI_CONFIG,
  CUSTOMER_UI_RENDERER_ERROR_REPORTER,
  CUSTOMER_VALIDATION_RULES,
} from './tokens';
import { CustomerRegistryUiConfig } from './models/config.model';
import { ExtraFieldDefinition } from './models/extensibility.model';

describe('provideCustomerRegistry', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('should provide default config when no options given', () => {
    TestBed.configureTestingModule({
      providers: [provideCustomerRegistry()],
    });

    const config = TestBed.inject(CUSTOMER_REGISTRY_UI_CONFIG);
    expect(config.apiBaseUrl).toBe('/api/v1');
    expect(config.locale).toBe('pt-BR');
    expect(config.features.search).toBe(true);
    expect(config.features.inlineEdit).toBe(false);
  });

  it('should merge partial config with defaults', () => {
    TestBed.configureTestingModule({
      providers: [
        provideCustomerRegistry({
          config: { apiBaseUrl: '/custom-api', features: { inlineEdit: true } },
        }),
      ],
    });

    const config: CustomerRegistryUiConfig = TestBed.inject(
      CUSTOMER_REGISTRY_UI_CONFIG
    );
    expect(config.apiBaseUrl).toBe('/custom-api');
    expect(config.locale).toBe('pt-BR'); // default preserved
    expect(config.features.inlineEdit).toBe(true); // overridden
    expect(config.features.search).toBe(true); // default preserved
  });

  it('should provide extra fields when specified', () => {
    const extraFields: ExtraFieldDefinition[] = [
      { key: 'loyalty', labelKey: 'field.loyalty', type: 'text' },
    ];

    TestBed.configureTestingModule({
      providers: [provideCustomerRegistry({ extraFields })],
    });

    const fields = TestBed.inject(CUSTOMER_EXTRA_FIELDS);
    // Multi-token returns array of arrays
    expect(fields).toEqual([extraFields]);
  });

  it('should provide i18n overrides when specified', () => {
    const overrides = { 'pt-BR': { 'label.customer': 'Consumidor' } };

    TestBed.configureTestingModule({
      providers: [provideCustomerRegistry({ i18nOverrides: overrides })],
    });

    const result = TestBed.inject(CUSTOMER_I18N_OVERRIDES);
    expect(result).toEqual(overrides);
  });

  it('should provide custom error reporter when specified', () => {
    const reporter = { report: jest.fn() };

    TestBed.configureTestingModule({
      providers: [provideCustomerRegistry({ errorReporter: reporter })],
    });

    const result = TestBed.inject(CUSTOMER_UI_RENDERER_ERROR_REPORTER);
    expect(result).toBe(reporter);
  });

  it('should not provide optional multi-tokens when not specified', () => {
    TestBed.configureTestingModule({
      providers: [provideCustomerRegistry()],
    });

    // Multi-tokens with no providers should throw (no default)
    expect(() => TestBed.inject(CUSTOMER_EXTRA_FIELDS)).toThrow();
    expect(() => TestBed.inject(CUSTOMER_VALIDATION_RULES)).toThrow();
    expect(() => TestBed.inject(CUSTOMER_FIELD_RENDERERS)).toThrow();
  });
});
