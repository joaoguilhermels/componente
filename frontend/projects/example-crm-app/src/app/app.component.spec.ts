import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideCustomerRegistry,
  CUSTOMER_UI_RENDERER_ERROR_REPORTER,
} from 'customer-registry-ui';
import { AppComponent } from './app.component';
import { LoyaltyRendererComponent } from './loyalty-renderer.component';
import { BuggyRendererComponent } from './buggy-renderer.component';
import { TrackingErrorReporter } from './tracking-error-reporter';

describe('AppComponent', () => {
  let component: AppComponent;
  let fixture: ComponentFixture<AppComponent>;
  let errorReporter: TrackingErrorReporter;

  beforeEach(async () => {
    errorReporter = new TrackingErrorReporter();

    await TestBed.configureTestingModule({
      imports: [AppComponent, NoopAnimationsModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideCustomerRegistry({
          config: { apiBaseUrl: '/api/v1', locale: 'en' },
          extraFields: [
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
          ],
          fieldRenderers: [
            { rendererId: 'loyalty-renderer', component: LoyaltyRendererComponent },
            { rendererId: 'buggy-renderer', component: BuggyRendererComponent },
          ],
          i18nOverrides: {
            en: {
              'field.loyaltyNumber': 'Loyalty Number',
              'field.buggyField': 'Buggy Field',
              'app.title': 'Example CRM System',
            },
          },
          errorReporter,
        }),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AppComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create the app', () => {
    expect(component).toBeTruthy();
  });

  it('should render the title with i18n override', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toContain('Example CRM System');
  });

  it('should render the customer form', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('crui-customer-form')).toBeTruthy();
  });

  it('should render the search component', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('crui-customer-search')).toBeTruthy();
  });
});
