import { TestBed } from '@angular/core/testing';
import { CustomerI18nService } from './customer-i18n.service';
import {
  CUSTOMER_I18N_OVERRIDES,
  CUSTOMER_REGISTRY_UI_CONFIG,
} from '../tokens';
import { DEFAULT_CONFIG } from '../models/config.model';

describe('CustomerI18nService', () => {
  function createService(
    locale = 'pt-BR',
    overrides: Record<string, Record<string, string>> = {}
  ): CustomerI18nService {
    TestBed.configureTestingModule({
      providers: [
        CustomerI18nService,
        {
          provide: CUSTOMER_REGISTRY_UI_CONFIG,
          useValue: { ...DEFAULT_CONFIG, locale },
        },
        { provide: CUSTOMER_I18N_OVERRIDES, useValue: overrides },
      ],
    });
    return TestBed.inject(CustomerI18nService);
  }

  afterEach(() => TestBed.resetTestingModule());

  describe('default locale (pt-BR)', () => {
    it('should translate known keys to Portuguese', () => {
      const service = createService('pt-BR');
      expect(service.translate('label.customer')).toBe('Cliente');
    });

    it('should return key when translation is missing', () => {
      const service = createService('pt-BR');
      expect(service.translate('nonexistent.key')).toBe('nonexistent.key');
    });
  });

  describe('locale switching', () => {
    it('should switch to English', () => {
      const service = createService('pt-BR');
      service.setLocale('en');
      expect(service.translate('label.customer')).toBe('Customer');
    });

    it('should fall back to en then pt-BR for unknown locale', () => {
      const service = createService('pt-BR');
      service.setLocale('fr');
      // Should fall back: fr (empty) → en → pt-BR
      expect(service.translate('label.customer')).toBe('Customer');
    });

    it('should update currentLocale signal', () => {
      const service = createService('pt-BR');
      expect(service.currentLocale()).toBe('pt-BR');
      service.setLocale('en');
      expect(service.currentLocale()).toBe('en');
    });
  });

  describe('host overrides', () => {
    it('should prefer host overrides over built-in', () => {
      const service = createService('pt-BR', {
        'pt-BR': { 'label.customer': 'Consumidor' },
      });
      expect(service.translate('label.customer')).toBe('Consumidor');
    });

    it('should only override specified keys', () => {
      const service = createService('pt-BR', {
        'pt-BR': { 'label.customer': 'Consumidor' },
      });
      // Non-overridden key still uses built-in
      expect(service.translate('label.create')).toBe('Criar');
    });
  });

  describe('parameter substitution', () => {
    it('should replace positional parameters', () => {
      const service = createService('pt-BR');
      const result = service.translate('validation.minLength', 5);
      expect(result).toBe('Mínimo de 5 caracteres.');
    });

    it('should handle multiple parameters', () => {
      const service = createService('pt-BR', {
        'pt-BR': { 'test.multi': 'From {0} to {1}' },
      });
      expect(service.translate('test.multi', 'A', 'B')).toBe('From A to B');
    });

    it('should replace all occurrences of the same parameter', () => {
      const service = createService('pt-BR', {
        'pt-BR': { 'test.repeat': 'Delete {0}? Deleting {0} is permanent.' },
      });
      expect(service.translate('test.repeat', 'X')).toBe(
        'Delete X? Deleting X is permanent.'
      );
    });
  });

  describe('onMissingKey callback', () => {
    it('should call onMissingKey when translation is not found', () => {
      const onMissingKey = jest.fn().mockReturnValue('Fallback Value');
      TestBed.configureTestingModule({
        providers: [
          CustomerI18nService,
          {
            provide: CUSTOMER_REGISTRY_UI_CONFIG,
            useValue: { ...DEFAULT_CONFIG, locale: 'pt-BR', onMissingKey },
          },
          { provide: CUSTOMER_I18N_OVERRIDES, useValue: {} },
        ],
      });
      const service = TestBed.inject(CustomerI18nService);

      const result = service.translate('nonexistent.key');

      expect(onMissingKey).toHaveBeenCalledWith('nonexistent.key', 'pt-BR');
      expect(result).toBe('Fallback Value');
    });

    it('should fall back to raw key when onMissingKey returns undefined', () => {
      const onMissingKey = jest.fn().mockReturnValue(undefined);
      TestBed.configureTestingModule({
        providers: [
          CustomerI18nService,
          {
            provide: CUSTOMER_REGISTRY_UI_CONFIG,
            useValue: { ...DEFAULT_CONFIG, locale: 'pt-BR', onMissingKey },
          },
          { provide: CUSTOMER_I18N_OVERRIDES, useValue: {} },
        ],
      });
      const service = TestBed.inject(CustomerI18nService);

      const result = service.translate('nonexistent.key');

      expect(onMissingKey).toHaveBeenCalledWith('nonexistent.key', 'pt-BR');
      expect(result).toBe('nonexistent.key');
    });

    it('should not call onMissingKey for existing keys', () => {
      const onMissingKey = jest.fn();
      TestBed.configureTestingModule({
        providers: [
          CustomerI18nService,
          {
            provide: CUSTOMER_REGISTRY_UI_CONFIG,
            useValue: { ...DEFAULT_CONFIG, locale: 'pt-BR', onMissingKey },
          },
          { provide: CUSTOMER_I18N_OVERRIDES, useValue: {} },
        ],
      });
      const service = TestBed.inject(CustomerI18nService);

      service.translate('label.customer');

      expect(onMissingKey).not.toHaveBeenCalled();
    });
  });

  describe('translations signal', () => {
    it('should recompute translations when locale changes', () => {
      const service = createService('pt-BR');
      const ptTranslations = service.translations();
      expect(ptTranslations['label.customer']).toBe('Cliente');

      service.setLocale('en');
      const enTranslations = service.translations();
      expect(enTranslations['label.customer']).toBe('Customer');
    });
  });
});
