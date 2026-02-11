import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  inject,
  Output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslatePipe } from '../../i18n/translate.pipe';
import { CUSTOMER_REGISTRY_UI_CONFIG } from '../../tokens';
import { CustomerSearchParams } from '../../models/customer.model';

@Component({
  selector: 'crui-customer-search',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    TranslatePipe,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (isVisible) {
      <div class="crui-search-container" [formGroup]="searchForm">
        <mat-form-field appearance="outline" class="crui-search-field">
          <mat-label>{{ 'field.type' | translate }}</mat-label>
          <mat-select formControlName="type">
            <mat-option [value]="undefined">{{ 'label.all' | translate }}</mat-option>
            <mat-option value="PF">{{ 'customer.type.PF' | translate }}</mat-option>
            <mat-option value="PJ">{{ 'customer.type.PJ' | translate }}</mat-option>
          </mat-select>
        </mat-form-field>

        <mat-form-field appearance="outline" class="crui-search-field">
          <mat-label>{{ 'field.status' | translate }}</mat-label>
          <mat-select formControlName="status">
            <mat-option [value]="undefined">{{ 'label.all' | translate }}</mat-option>
            <mat-option value="DRAFT">{{ 'customer.status.DRAFT' | translate }}</mat-option>
            <mat-option value="ACTIVE">{{ 'customer.status.ACTIVE' | translate }}</mat-option>
            <mat-option value="SUSPENDED">{{ 'customer.status.SUSPENDED' | translate }}</mat-option>
            <mat-option value="CLOSED">{{ 'customer.status.CLOSED' | translate }}</mat-option>
          </mat-select>
        </mat-form-field>

        <mat-form-field appearance="outline" class="crui-search-field">
          <mat-label>{{ 'field.document' | translate }}</mat-label>
          <input matInput formControlName="document" />
        </mat-form-field>

        <mat-form-field appearance="outline" class="crui-search-field">
          <mat-label>{{ 'field.displayName' | translate }}</mat-label>
          <input matInput formControlName="displayName" />
        </mat-form-field>

        <div class="crui-search-actions">
          <button mat-flat-button color="primary" (click)="onSearch()">
            <mat-icon>search</mat-icon>
            {{ 'label.search' | translate }}
          </button>
          <button mat-stroked-button (click)="onReset()">
            {{ 'label.clear' | translate }}
          </button>
        </div>
      </div>
    }
  `,
  styles: [`
    .crui-search-container {
      display: flex;
      flex-wrap: wrap;
      gap: var(--crui-spacing-sm, 8px);
      align-items: flex-start;
      padding: var(--crui-spacing-md, 16px) 0;
    }
    .crui-search-field {
      flex: 1 1 200px;
      min-width: 180px;
    }
    .crui-search-actions {
      display: flex;
      gap: var(--crui-spacing-sm, 8px);
      align-items: center;
      padding-top: 4px;
    }
  `],
})
export class CustomerSearchComponent {
  private readonly config = inject(CUSTOMER_REGISTRY_UI_CONFIG);
  private readonly fb = inject(FormBuilder);

  /** Emits the search parameters when the user clicks the search button */
  @Output() readonly search = new EventEmitter<CustomerSearchParams>();

  /** Emits when the user clicks the clear/reset button */
  @Output() readonly reset = new EventEmitter<void>();

  readonly searchForm: FormGroup = this.fb.group({
    type: [undefined],
    status: [undefined],
    document: [''],
    displayName: [''],
  });

  /**
   * Whether the search panel is visible, based on the `search` feature flag.
   * Config is injected once via `useValue`; runtime changes are not reflected.
   */
  get isVisible(): boolean {
    return this.config.features.search;
  }

  onSearch(): void {
    const v = this.searchForm.value;
    const params: CustomerSearchParams = {};
    if (v.type) params.type = v.type;
    if (v.status) params.status = v.status;
    if (v.document?.trim()) params.document = v.document.trim();
    if (v.displayName?.trim()) params.displayName = v.displayName.trim();
    this.search.emit(params);
  }

  onReset(): void {
    this.searchForm.reset({
      type: undefined,
      status: undefined,
      document: '',
      displayName: '',
    });
    this.reset.emit();
  }
}
