import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  inject,
  input,
  Output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatDividerModule } from '@angular/material/divider';
import { MatChipsModule } from '@angular/material/chips';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslatePipe } from '../../i18n/translate.pipe';
import { CUSTOMER_REGISTRY_UI_CONFIG } from '../../tokens';
import { Customer } from '../../models/customer.model';

@Component({
  selector: 'crui-customer-details',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatDividerModule,
    MatChipsModule,
    MatButtonModule,
    MatIconModule,
    TranslatePipe,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (customer(); as c) {
      <mat-card class="crui-details-card">
        <mat-card-header>
          <mat-card-title>{{ c.displayName }}</mat-card-title>
          <mat-card-subtitle>
            {{ 'customer.type.' + c.type | translate }} &mdash;
            <span class="crui-status-badge crui-status-{{ c.status | lowercase }}">
              {{ 'customer.status.' + c.status | translate }}
            </span>
          </mat-card-subtitle>
        </mat-card-header>

        <mat-card-content>
          <div class="crui-detail-grid">
            <div class="crui-detail-field">
              <span class="crui-detail-label">{{ 'field.document' | translate }}</span>
              <span class="crui-detail-value">{{ c.document }}</span>
            </div>
            <div class="crui-detail-field">
              <span class="crui-detail-label">{{ 'field.createdAt' | translate }}</span>
              <span class="crui-detail-value">{{ c.createdAt | date:'medium' }}</span>
            </div>
            <div class="crui-detail-field">
              <span class="crui-detail-label">{{ 'field.updatedAt' | translate }}</span>
              <span class="crui-detail-value">{{ c.updatedAt | date:'medium' }}</span>
            </div>
          </div>

          @if (showAddresses && c.addresses.length > 0) {
            <mat-divider></mat-divider>
            <h3 class="crui-section-title">{{ 'field.addresses' | translate }}</h3>
            @for (address of c.addresses; track address.id) {
              <div class="crui-address-item">
                <span>{{ address.street }}, {{ address.number }}</span>
                @if (address.complement) {
                  <span> - {{ address.complement }}</span>
                }
                <br/>
                <span>{{ address.neighborhood }}, {{ address.city }} - {{ address.state }}</span>
                <br/>
                <span>{{ address.zipCode }} - {{ address.country }}</span>
              </div>
            }
          }

          @if (showContacts && c.contacts.length > 0) {
            <mat-divider></mat-divider>
            <h3 class="crui-section-title">{{ 'field.contacts' | translate }}</h3>
            @for (contact of c.contacts; track contact.id) {
              <div class="crui-contact-item">
                <mat-chip-set>
                  <mat-chip [highlighted]="contact.primary">
                    {{ 'contact.type.' + contact.type | translate }}: {{ contact.value }}
                    @if (contact.primary) {
                      <mat-icon matChipTrailingIcon>star</mat-icon>
                    }
                  </mat-chip>
                </mat-chip-set>
              </div>
            }
          }
        </mat-card-content>

        <mat-card-actions align="end">
          <button mat-button (click)="edit.emit(c)">
            <mat-icon>edit</mat-icon>
            {{ 'label.edit' | translate }}
          </button>
          <button mat-button (click)="back.emit()">
            {{ 'label.back' | translate }}
          </button>
        </mat-card-actions>
      </mat-card>
    }
  `,
  styles: [`
    .crui-details-card {
      max-width: 800px;
    }
    .crui-detail-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: var(--crui-spacing-md, 16px);
      padding: var(--crui-spacing-md, 16px) 0;
    }
    .crui-detail-field {
      display: flex;
      flex-direction: column;
    }
    .crui-detail-label {
      font-size: 12px;
      color: var(--crui-text-secondary, rgba(0, 0, 0, 0.54));
      margin-bottom: 4px;
    }
    .crui-detail-value {
      font-size: 14px;
    }
    .crui-section-title {
      margin: var(--crui-spacing-md, 16px) 0 var(--crui-spacing-sm, 8px);
      font-size: 16px;
      font-weight: 500;
    }
    .crui-address-item, .crui-contact-item {
      margin-bottom: var(--crui-spacing-sm, 8px);
    }
    .crui-status-badge {
      padding: 2px 8px;
      border-radius: 12px;
      font-size: 12px;
      font-weight: 500;
    }
    .crui-status-draft { background: #e0e0e0; }
    .crui-status-active { background: #c8e6c9; color: #2e7d32; }
    .crui-status-suspended { background: #fff9c4; color: #f57f17; }
    .crui-status-closed { background: #ffcdd2; color: #c62828; }
  `],
})
export class CustomerDetailsComponent {
  private readonly config = inject(CUSTOMER_REGISTRY_UI_CONFIG);

  /** The customer to display. When null, the component renders nothing. */
  readonly customer = input<Customer | null>(null);

  /** Emits the customer when the user clicks the edit button */
  @Output() readonly edit = new EventEmitter<Customer>();

  /** Emits when the user clicks the back button */
  @Output() readonly back = new EventEmitter<void>();

  /** Whether the addresses section is enabled by the feature flag */
  get showAddresses(): boolean {
    return this.config.features.addresses;
  }

  /** Whether the contacts section is enabled by the feature flag */
  get showContacts(): boolean {
    return this.config.features.contacts;
  }
}
