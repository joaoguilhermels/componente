import { inject, Pipe, PipeTransform } from '@angular/core';
import { CustomerI18nService } from './customer-i18n.service';

/**
 * Standalone pipe for translating i18n keys in templates.
 *
 * Usage: {{ 'label.customer' | translate }}
 * With params: {{ 'validation.minLength' | translate:3 }}
 *
 * Marked as impure so Angular re-evaluates on every change detection
 * cycle, allowing locale switches to take effect immediately.
 * Internal caching (locale + key + params) prevents redundant lookups
 * when neither the locale nor the inputs have changed.
 */
@Pipe({
  name: 'translate',
  standalone: true,
  pure: false,
})
export class TranslatePipe implements PipeTransform {
  private readonly i18n = inject(CustomerI18nService);

  private cachedLocale: string | null = null;
  private cachedKey: string | null = null;
  private cachedParams: string | null = null;
  private cachedResult = '';

  transform(key: string, ...params: (string | number)[]): string {
    const locale = this.i18n.currentLocale();
    const paramsKey = params.length > 0 ? JSON.stringify(params) : '';

    if (
      key === this.cachedKey &&
      locale === this.cachedLocale &&
      paramsKey === this.cachedParams
    ) {
      return this.cachedResult;
    }

    this.cachedLocale = locale;
    this.cachedKey = key;
    this.cachedParams = paramsKey;
    this.cachedResult = this.i18n.translate(key, ...params);
    return this.cachedResult;
  }
}
