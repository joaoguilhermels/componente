import { signal, WritableSignal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { TranslatePipe } from './translate.pipe';
import { CustomerI18nService } from './customer-i18n.service';

describe('TranslatePipe', () => {
  let pipe: TranslatePipe;
  let i18nService: jest.Mocked<CustomerI18nService>;
  let localeSignal: WritableSignal<string>;
  let versionSignal: WritableSignal<number>;

  beforeEach(() => {
    localeSignal = signal('pt-BR');
    versionSignal = signal(0);
    const mockService = {
      currentLocale: localeSignal.asReadonly(),
      translationsVersion: versionSignal.asReadonly(),
      translate: jest.fn((key: string, ...params: (string | number)[]) => {
        const locale = localeSignal();
        if (locale === 'pt-BR') {
          if (key === 'label.customer') return 'Cliente';
          if (key === 'validation.minLength') return `Mínimo de ${params[0]} caracteres.`;
        }
        if (locale === 'en-US') {
          if (key === 'label.customer') return 'Customer';
          if (key === 'validation.minLength') return `Minimum ${params[0]} characters.`;
        }
        return key;
      }),
    };

    TestBed.configureTestingModule({
      providers: [
        TranslatePipe,
        { provide: CustomerI18nService, useValue: mockService },
      ],
    });

    pipe = TestBed.inject(TranslatePipe);
    i18nService = TestBed.inject(CustomerI18nService) as jest.Mocked<CustomerI18nService>;
  });

  afterEach(() => TestBed.resetTestingModule());

  it('should translate a simple key', () => {
    expect(pipe.transform('label.customer')).toBe('Cliente');
    expect(i18nService.translate).toHaveBeenCalledWith('label.customer');
  });

  it('should pass parameters to translate', () => {
    expect(pipe.transform('validation.minLength', 5)).toBe('Mínimo de 5 caracteres.');
    expect(i18nService.translate).toHaveBeenCalledWith('validation.minLength', 5);
  });

  it('should return key when no translation found', () => {
    expect(pipe.transform('unknown.key')).toBe('unknown.key');
  });

  it('should use cache when key and locale are unchanged', () => {
    pipe.transform('label.customer');
    pipe.transform('label.customer');

    // translate should only be called once — second call uses cache
    expect(i18nService.translate).toHaveBeenCalledTimes(1);
  });

  it('should re-translate when locale changes', () => {
    const first = pipe.transform('label.customer');
    expect(first).toBe('Cliente');

    localeSignal.set('en-US');
    const second = pipe.transform('label.customer');
    expect(second).toBe('Customer');

    expect(i18nService.translate).toHaveBeenCalledTimes(2);
  });

  it('should invalidate cache when translations version changes (C5)', () => {
    pipe.transform('label.customer');
    expect(i18nService.translate).toHaveBeenCalledTimes(1);

    // Same key and locale but version bumped
    versionSignal.set(1);
    pipe.transform('label.customer');
    expect(i18nService.translate).toHaveBeenCalledTimes(2);
  });
});
