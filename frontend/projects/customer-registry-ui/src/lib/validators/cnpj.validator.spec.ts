import { FormControl } from '@angular/forms';
import { cnpjValidator } from './cnpj.validator';

describe('cnpjValidator', () => {
  const validator = cnpjValidator();
  const control = new FormControl('');

  function validate(value: string | null) {
    control.setValue(value);
    return validator(control);
  }

  it('should return null for empty value (defer to required)', () => {
    expect(validate('')).toBeNull();
    expect(validate(null)).toBeNull();
  });

  it('should accept a valid CNPJ (digits only)', () => {
    expect(validate('11222333000181')).toBeNull();
  });

  it('should accept a valid CNPJ (formatted)', () => {
    expect(validate('11.222.333/0001-81')).toBeNull();
  });

  it('should reject CNPJ with wrong check digits', () => {
    expect(validate('11222333000199')).toEqual({ cnpjInvalid: true });
  });

  it('should reject CNPJ with wrong length', () => {
    expect(validate('1122233300018')).toEqual({ cnpjInvalid: true });
    expect(validate('112223330001811')).toEqual({ cnpjInvalid: true });
  });

  it('should reject all-same-digit CNPJs', () => {
    expect(validate('11111111111111')).toEqual({ cnpjInvalid: true });
    expect(validate('00000000000000')).toEqual({ cnpjInvalid: true });
  });

  it('should accept another valid CNPJ', () => {
    // Known valid: 11.444.777/0001-61
    expect(validate('11444777000161')).toBeNull();
  });
});
