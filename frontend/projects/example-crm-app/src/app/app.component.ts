import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  CustomerFormComponent,
  CustomerListComponent,
  CustomerSearchComponent,
  TranslatePipe,
  CreateCustomerRequest,
  CustomerSearchParams,
} from 'customer-registry-ui';

/**
 * Root component of the example CRM application.
 * Demonstrates consuming the customer-registry-ui library
 * with custom fields, renderers, and i18n overrides.
 */
@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    CustomerFormComponent,
    CustomerSearchComponent,
    CustomerListComponent,
    TranslatePipe,
  ],
  template: `
    <div class="app-container">
      <h1>{{ 'app.title' | translate }}</h1>

      <section class="app-section">
        <h2>{{ 'label.create' | translate }}</h2>
        <crui-customer-form
          (submitForm)="onFormSubmit($event)"
          (cancel)="onFormCancel()">
        </crui-customer-form>
      </section>

      <section class="app-section">
        <h2>{{ 'label.search' | translate }}</h2>
        <crui-customer-search
          (search)="onSearch($event)">
        </crui-customer-search>
      </section>
    </div>
  `,
  styles: [`
    .app-container {
      max-width: 1200px;
      margin: 0 auto;
      padding: 24px;
    }
    .app-section {
      margin-bottom: 32px;
    }
  `],
})
export class AppComponent {
  onFormSubmit(data: CreateCustomerRequest): void {
    console.log('Form submitted:', data);
  }

  onFormCancel(): void {
    console.log('Form cancelled');
  }

  onSearch(params: CustomerSearchParams): void {
    console.log('Search params:', params);
  }
}
