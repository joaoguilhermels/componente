import { computed, inject, Injectable, isDevMode, signal } from '@angular/core';
import {
  CUSTOMER_I18N_OVERRIDES,
  CUSTOMER_REGISTRY_UI_CONFIG,
} from '../tokens';
import { LOCALE_EN } from './locale-en';
import { LOCALE_PT_BR } from './locale-pt-br';

const BUILT_IN_LOCALES: Record<string, Record<string, string>> = {
  'pt-BR': LOCALE_PT_BR,
  en: LOCALE_EN,
};

/**
 * Signal-based i18n service for the Customer Registry UI library.
 *
 * Fallback chain: host overrides → built-in[locale] → built-in['en'] → built-in['pt-BR'] → key
 *
 * **Limitations**: This service supports positional parameter substitution (`{0}`, `{1}`, ...)
 * only. It does **not** support ICU MessageFormat, pluralization rules, or gender-aware
 * translations. For those features, host applications should use a full i18n library
 * (e.g., `@ngx-translate/core` or Angular's built-in i18n) and provide translations
 * via `CUSTOMER_I18N_OVERRIDES`.
 */
@Injectable({ providedIn: 'root' })
export class CustomerI18nService {
  private readonly config = inject(CUSTOMER_REGISTRY_UI_CONFIG);
  private readonly hostOverrides = inject(CUSTOMER_I18N_OVERRIDES);

  /** Current locale signal — reactive, can be changed at runtime */
  readonly currentLocale = signal<string>(this.config.locale);

  /** Monotonically increasing version counter, incremented on every locale change */
  private readonly _translationsVersion = signal(0);
  readonly translationsVersion = this._translationsVersion.asReadonly();

  /** Computed translations map for the current locale */
  readonly translations = computed<Record<string, string>>(() => {
    const locale = this.currentLocale();
    return this.resolveTranslations(locale);
  });

  /**
   * Translate a key, with optional parameter substitution.
   * Parameters are positional: {0}, {1}, etc.
   */
  translate(key: string, ...params: (string | number)[]): string {
    const map = this.translations();
    let value = map[key];

    if (value === undefined) {
      const locale = this.currentLocale();
      const onMissingKey = this.config.onMissingKey;
      if (onMissingKey) {
        const override = onMissingKey(key, locale);
        if (override !== undefined) {
          value = override;
        }
      }
      if (value === undefined) {
        if (isDevMode()) {
          console.warn(
            `[CustomerRegistryUI i18n] Missing translation key "${key}" for locale "${locale}"`
          );
        }
        value = key;
      }
    }

    params.forEach((param, i) => {
      value = value!.replaceAll(`{${i}}`, String(param));
    });
    return value;
  }

  /** Switch the active locale at runtime */
  setLocale(locale: string): void {
    this.currentLocale.set(locale);
    this._translationsVersion.update(v => v + 1);
  }

  private resolveTranslations(locale: string): Record<string, string> {
    const overrides = this.hostOverrides[locale] ?? {};
    const builtIn = BUILT_IN_LOCALES[locale] ?? {};
    const fallbackEn = BUILT_IN_LOCALES['en'] ?? {};
    const fallbackPtBr = BUILT_IN_LOCALES['pt-BR'] ?? {};

    return { ...fallbackPtBr, ...fallbackEn, ...builtIn, ...overrides };
  }
}
