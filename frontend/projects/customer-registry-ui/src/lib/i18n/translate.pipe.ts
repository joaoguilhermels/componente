import { inject, Pipe, PipeTransform } from '@angular/core';
import { CustomerI18nService } from './customer-i18n.service';

/**
 * Standalone pipe for translating i18n keys in templates.
 *
 * Usage: {{ 'label.customer' | translate }}
 * With params: {{ 'validation.minLength' | translate:3 }}
 *
 * Marked as pure with internal caching. The pipe reads the
 * currentLocale() signal on each invocation and compares it to the
 * cached locale, so locale switches invalidate the cache correctly.
 */
@Pipe({
  name: 'translate',
  standalone: true,
  pure: true,
})
export class TranslatePipe implements PipeTransform {
  private readonly i18n = inject(CustomerI18nService);

  private cachedLocale: string | null = null;
  private cachedKey: string | null = null;
  private cachedParams: string | null = null;
  private cachedVersion: number | null = null;
  private cachedResult = '';

  transform(key: string, ...params: (string | number)[]): string {
    const locale = this.i18n.currentLocale();
    const version = this.i18n.translationsVersion();
    const paramsKey = params.length > 0 ? params.join('|') : '';

    if (
      key === this.cachedKey &&
      locale === this.cachedLocale &&
      paramsKey === this.cachedParams &&
      version === this.cachedVersion
    ) {
      return this.cachedResult;
    }

    this.cachedLocale = locale;
    this.cachedKey = key;
    this.cachedParams = paramsKey;
    this.cachedVersion = version;
    this.cachedResult = this.i18n.translate(key, ...params);
    return this.cachedResult;
  }
}
