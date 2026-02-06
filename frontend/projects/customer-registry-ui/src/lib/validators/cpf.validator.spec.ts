import { FormControl } from '@angular/forms';
import { cpfValidator } from './cpf.validator';

describe('cpfValidator', () => {
  const validator = cpfValidator();
  const control = new FormControl('');

  function validate(value: string | null) {
    control.setValue(value);
    return validator(control);
  }

  it('should return null for empty value (defer to required)', () => {
    expect(validate('')).toBeNull();
    expect(validate(null)).toBeNull();
  });

  it('should accept a valid CPF (digits only)', () => {
    expect(validate('52998224725')).toBeNull();
  });

  it('should accept a valid CPF (formatted)', () => {
    expect(validate('529.982.247-25')).toBeNull();
  });

  it('should reject CPF with wrong check digits', () => {
    expect(validate('52998224700')).toEqual({ cpfInvalid: true });
  });

  it('should reject CPF with wrong length', () => {
    expect(validate('1234567890')).toEqual({ cpfInvalid: true });
    expect(validate('123456789012')).toEqual({ cpfInvalid: true });
  });

  it('should reject all-same-digit CPFs', () => {
    expect(validate('11111111111')).toEqual({ cpfInvalid: true });
    expect(validate('00000000000')).toEqual({ cpfInvalid: true });
    expect(validate('99999999999')).toEqual({ cpfInvalid: true });
  });

  it('should accept another valid CPF', () => {
    // Known valid CPF: 111.444.777-35
    expect(validate('11144477735')).toBeNull();
  });
});
