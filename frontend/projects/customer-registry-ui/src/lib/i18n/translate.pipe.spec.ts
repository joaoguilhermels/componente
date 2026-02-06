import { TestBed } from '@angular/core/testing';
import { TranslatePipe } from './translate.pipe';
import { CustomerI18nService } from './customer-i18n.service';

describe('TranslatePipe', () => {
  let pipe: TranslatePipe;
  let i18nService: jest.Mocked<CustomerI18nService>;

  beforeEach(() => {
    const mockService = {
      translate: jest.fn((key: string, ...params: (string | number)[]) => {
        if (key === 'label.customer') return 'Cliente';
        if (key === 'validation.minLength') return `Mínimo de ${params[0]} caracteres.`;
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
});
