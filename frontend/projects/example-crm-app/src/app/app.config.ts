import { ApplicationConfig } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import {
  provideCustomerRegistry,
  ExtraFieldDefinition,
  FieldRendererRegistration,
} from 'customer-registry-ui';
import { LoyaltyRendererComponent } from './loyalty-renderer.component';
import { BuggyRendererComponent } from './buggy-renderer.component';
import { TrackingErrorReporter } from './tracking-error-reporter';

/**
 * Extra fields injected into the customer form.
 * loyaltyNumber: uses a custom renderer with a star icon.
 * buggyField: intentionally uses a broken renderer to demo graceful degradation.
 */
const extraFields: ExtraFieldDefinition[] = [
  {
    key: 'loyaltyNumber',
    labelKey: 'field.loyaltyNumber',
    type: 'custom',
    rendererId: 'loyalty-renderer',
  },
  {
    key: 'buggyField',
    labelKey: 'field.buggyField',
    type: 'custom',
    rendererId: 'buggy-renderer',
  },
];

const fieldRenderers: FieldRendererRegistration[] = [
  { rendererId: 'loyalty-renderer', component: LoyaltyRendererComponent },
  { rendererId: 'buggy-renderer', component: BuggyRendererComponent },
];

const i18nOverrides: Record<string, Record<string, string>> = {
  'pt-BR': {
    'field.loyaltyNumber': 'Número de Fidelidade',
    'field.buggyField': 'Campo com Bug',
    'app.title': 'Sistema CRM Exemplo',
  },
  en: {
    'field.loyaltyNumber': 'Loyalty Number',
    'field.buggyField': 'Buggy Field',
    'app.title': 'Example CRM System',
  },
};

export const trackingErrorReporter = new TrackingErrorReporter();

export const appConfig: ApplicationConfig = {
  providers: [
    provideHttpClient(),
    provideAnimations(),
    provideCustomerRegistry({
      config: {
        apiBaseUrl: '/api/v1',
        locale: 'pt-BR',
        features: {
          search: true,
          list: true,
          details: true,
          form: true,
        },
      },
      extraFields,
      fieldRenderers,
      i18nOverrides,
      errorReporter: trackingErrorReporter,
    }),
  ],
};
