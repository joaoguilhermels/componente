import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/**
 * Angular ValidatorFn that validates a Brazilian CPF (11 digits).
 * Strips non-digit characters before validation.
 * Returns { cpfInvalid: true } on failure, null on success.
 */
export function cpfValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value;
    if (!value) return null; // let required validator handle empty

    const digits = String(value).replace(/\D/g, '');
    if (!isValidCpf(digits)) {
      return { cpfInvalid: true };
    }
    return null;
  };
}

function isValidCpf(digits: string): boolean {
  if (digits.length !== 11) return false;

  // Reject all-same-digit CPFs (e.g. 111.111.111-11)
  if (/^(\d)\1{10}$/.test(digits)) return false;

  // First check digit
  let sum = 0;
  for (let i = 0; i < 9; i++) {
    sum += parseInt(digits[i], 10) * (10 - i);
  }
  let remainder = (sum * 10) % 11;
  if (remainder === 10) remainder = 0;
  if (remainder !== parseInt(digits[9], 10)) return false;

  // Second check digit
  sum = 0;
  for (let i = 0; i < 10; i++) {
    sum += parseInt(digits[i], 10) * (11 - i);
  }
  remainder = (sum * 10) % 11;
  if (remainder === 10) remainder = 0;
  if (remainder !== parseInt(digits[10], 10)) return false;

  return true;
}
