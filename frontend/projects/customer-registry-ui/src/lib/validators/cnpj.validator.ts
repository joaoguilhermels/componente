import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/**
 * Angular ValidatorFn that validates a Brazilian CNPJ (14 digits).
 * Strips non-digit characters before validation.
 * Returns { cnpjInvalid: true } on failure, null on success.
 */
export function cnpjValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value;
    if (!value) return null; // let required validator handle empty

    const digits = String(value).replace(/\D/g, '');
    if (!isValidCnpj(digits)) {
      return { cnpjInvalid: true };
    }
    return null;
  };
}

const CNPJ_WEIGHTS_1 = [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];
const CNPJ_WEIGHTS_2 = [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];

function isValidCnpj(digits: string): boolean {
  if (digits.length !== 14) return false;

  // Reject all-same-digit CNPJs
  if (/^(\d)\1{13}$/.test(digits)) return false;

  // First check digit
  let sum = 0;
  for (let i = 0; i < 12; i++) {
    sum += parseInt(digits[i], 10) * CNPJ_WEIGHTS_1[i];
  }
  let remainder = sum % 11;
  const firstCheck = remainder < 2 ? 0 : 11 - remainder;
  if (firstCheck !== parseInt(digits[12], 10)) return false;

  // Second check digit
  sum = 0;
  for (let i = 0; i < 13; i++) {
    sum += parseInt(digits[i], 10) * CNPJ_WEIGHTS_2[i];
  }
  remainder = sum % 11;
  const secondCheck = remainder < 2 ? 0 : 11 - remainder;
  if (secondCheck !== parseInt(digits[13], 10)) return false;

  return true;
}
