import { LOCALE_EN } from './locale-en';
import { LOCALE_PT_BR } from './locale-pt-br';

describe('Locale key parity', () => {
  const enKeys = Object.keys(LOCALE_EN).sort();
  const ptBrKeys = Object.keys(LOCALE_PT_BR).sort();

  it('should have the same keys in en and pt-BR', () => {
    expect(enKeys).toEqual(ptBrKeys);
  });

  it('should not have keys in en missing from pt-BR', () => {
    const missingInPtBr = enKeys.filter((key) => !(key in LOCALE_PT_BR));
    expect(missingInPtBr).toEqual([]);
  });

  it('should not have keys in pt-BR missing from en', () => {
    const missingInEn = ptBrKeys.filter((key) => !(key in LOCALE_EN));
    expect(missingInEn).toEqual([]);
  });

  it('should not have empty values in en', () => {
    const emptyKeys = enKeys.filter((key) => LOCALE_EN[key].trim() === '');
    expect(emptyKeys).toEqual([]);
  });

  it('should not have empty values in pt-BR', () => {
    const emptyKeys = ptBrKeys.filter((key) => LOCALE_PT_BR[key].trim() === '');
    expect(emptyKeys).toEqual([]);
  });
});
