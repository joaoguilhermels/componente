import {
  ChangeDetectionStrategy,
  Component,
  effect,
  EventEmitter,
  inject,
  input,
  OnInit,
  Output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatRadioModule } from '@angular/material/radio';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslatePipe } from '../../i18n/translate.pipe';
import { SafeFieldRendererHostComponent } from '../safe-field-renderer/safe-field-renderer-host.component';
import {
  Customer,
  CreateCustomerRequest,
  CustomerType,
} from '../../models/customer.model';
import {
  ExtraFieldDefinition,
  FieldRendererRegistration,
} from '../../models/extensibility.model';
import {
  CUSTOMER_EXTRA_FIELDS,
  CUSTOMER_FIELD_RENDERERS,
  CUSTOMER_VALIDATION_RULES,
} from '../../tokens';
import { CustomerValidationRule } from '../../models/extensibility.model';
import { cpfValidator } from '../../validators/cpf.validator';
import { cnpjValidator } from '../../validators/cnpj.validator';

@Component({
  selector: 'crui-customer-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatRadioModule,
    MatButtonModule,
    MatIconModule,
    TranslatePipe,
    SafeFieldRendererHostComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <form [formGroup]="form" (ngSubmit)="onSubmit()" class="crui-form">
      <div class="crui-form-row">
        <mat-radio-group formControlName="type" (change)="onTypeChange()">
          <mat-radio-button value="PF">{{ 'customer.type.PF' | translate }}</mat-radio-button>
          <mat-radio-button value="PJ">{{ 'customer.type.PJ' | translate }}</mat-radio-button>
        </mat-radio-group>
      </div>

      <div class="crui-form-row">
        <mat-form-field appearance="outline" class="crui-form-field">
          <mat-label>{{ 'field.document' | translate }}</mat-label>
          <input matInput formControlName="document" />
          @if (form.get('document')?.hasError('required')) {
            <mat-error>{{ 'validation.required' | translate }}</mat-error>
          }
          @if (form.get('document')?.hasError('cpfInvalid')) {
            <mat-error>{{ 'validation.cpf.invalid' | translate }}</mat-error>
          }
          @if (form.get('document')?.hasError('cnpjInvalid')) {
            <mat-error>{{ 'validation.cnpj.invalid' | translate }}</mat-error>
          }
        </mat-form-field>

        <mat-form-field appearance="outline" class="crui-form-field">
          <mat-label>{{ 'field.displayName' | translate }}</mat-label>
          <input matInput formControlName="displayName" />
          @if (form.get('displayName')?.hasError('required')) {
            <mat-error>{{ 'validation.required' | translate }}</mat-error>
          }
        </mat-form-field>
      </div>

      @for (field of visibleExtraFields; track field.key) {
        <div class="crui-form-row">
          @if (getRendererRegistration(field.rendererId); as registration) {
            <crui-safe-field-renderer
              [registration]="registration"
              [labelKey]="field.labelKey"
              [control]="getExtraFieldControl(field.key)"
              [fieldKey]="field.key"
              [disabled]="form.disabled">
            </crui-safe-field-renderer>
          } @else {
            @switch (field.type) {
              @case ('select') {
                <mat-form-field appearance="outline" class="crui-form-field">
                  <mat-label>{{ field.labelKey | translate }}</mat-label>
                  <mat-select [formControlName]="field.key">
                    @for (option of field.options ?? []; track option.value) {
                      <mat-option [value]="option.value">{{ option.labelKey | translate }}</mat-option>
                    }
                  </mat-select>
                </mat-form-field>
              }
              @default {
                <mat-form-field appearance="outline" class="crui-form-field">
                  <mat-label>{{ field.labelKey | translate }}</mat-label>
                  <input matInput [type]="field.type" [formControlName]="field.key" />
                </mat-form-field>
              }
            }
          }
        </div>
      }

      <div class="crui-form-actions">
        <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid">
          {{ (editMode() ? 'label.save' : 'label.create') | translate }}
        </button>
        <button mat-stroked-button type="button" (click)="cancel.emit()">
          {{ 'label.cancel' | translate }}
        </button>
      </div>
    </form>
  `,
  styles: [`
    .crui-form {
      display: flex;
      flex-direction: column;
      gap: var(--crui-spacing-md, 16px);
      max-width: 800px;
    }
    .crui-form-row {
      display: flex;
      flex-wrap: wrap;
      gap: var(--crui-spacing-sm, 8px);
    }
    .crui-form-field {
      flex: 1 1 250px;
    }
    .crui-form-actions {
      display: flex;
      gap: var(--crui-spacing-sm, 8px);
      padding-top: var(--crui-spacing-sm, 8px);
    }
    mat-radio-button {
      margin-right: var(--crui-spacing-md, 16px);
    }
  `],
})
export class CustomerFormComponent implements OnInit {
  /** Customer to edit. When null, the form operates in create mode. Default: null. */
  readonly customer = input<Customer | null>(null);

  /** Whether the form is in edit mode (disables type and document fields). Default: false. */
  readonly editMode = input(false);

  /** Emits the CreateCustomerRequest payload when the form is submitted */
  @Output() readonly submitForm = new EventEmitter<CreateCustomerRequest>();

  /** Emits when the user clicks the cancel button */
  @Output() readonly cancel = new EventEmitter<void>();

  private readonly fb = inject(FormBuilder);

  private readonly extraFieldArrays = inject<ExtraFieldDefinition[][]>(
    CUSTOMER_EXTRA_FIELDS, { optional: true }
  ) ?? [];
  private readonly validationRuleArrays = inject<CustomerValidationRule[][]>(
    CUSTOMER_VALIDATION_RULES, { optional: true }
  ) ?? [];
  private readonly rendererArrays = inject<FieldRendererRegistration[][]>(
    CUSTOMER_FIELD_RENDERERS, { optional: true }
  ) ?? [];

  /** Flattened extra fields from all providers */
  readonly extraFields: ExtraFieldDefinition[] = this.extraFieldArrays.flat();
  /** Flattened renderers from all providers */
  readonly renderers: FieldRendererRegistration[] = this.rendererArrays.flat();
  /** Flattened validation rules from all providers */
  readonly validationRules: CustomerValidationRule[] = this.validationRuleArrays.flat();

  form!: FormGroup;

  constructor() {
    effect(() => {
      const c = this.customer();
      if (this.form && c) {
        this.patchForm();
      }
    });
  }

  get currentType(): CustomerType {
    return this.form?.get('type')?.value ?? 'PF';
  }

  get visibleExtraFields(): ExtraFieldDefinition[] {
    return this.extraFields.filter(
      (f) => !f.appliesTo || f.appliesTo.includes(this.currentType)
    );
  }

  ngOnInit(): void {
    this.buildForm();
  }

  onTypeChange(): void {
    const documentControl = this.form.get('document')!;
    documentControl.clearValidators();
    documentControl.addValidators([Validators.required]);

    if (this.currentType === 'PF') {
      documentControl.addValidators([cpfValidator()]);
    } else {
      documentControl.addValidators([cnpjValidator()]);
    }
    documentControl.updateValueAndValidity();

    // Clear/re-apply extra field validators based on appliesTo
    for (const field of this.extraFields) {
      const control = this.form.get(field.key);
      if (!control) continue;
      control.clearValidators();
      if (!field.appliesTo || field.appliesTo.includes(this.currentType)) {
        if (field.validatorFns?.length) {
          control.addValidators(field.validatorFns);
        }
      }
      control.updateValueAndValidity();
    }

    // Clear/re-apply host validation rules based on appliesTo.
    // Collect unique field paths, rebuild their validators from base + applicable rules.
    const ruleFieldPaths = new Set(this.validationRules.map((r) => r.fieldPath));
    for (const fieldPath of ruleFieldPaths) {
      const control = this.form.get(fieldPath);
      if (!control) continue;
      // Skip document — already fully rebuilt above
      if (fieldPath === 'document') continue;

      // Rebuild: base required validator (for core fields) + applicable rules
      control.clearValidators();
      if (fieldPath === 'displayName') {
        control.addValidators([Validators.required]);
      }
      for (const rule of this.validationRules) {
        if (rule.fieldPath !== fieldPath) continue;
        if (!rule.appliesTo || rule.appliesTo.includes(this.currentType)) {
          control.addValidators(rule.validators);
        }
      }
      control.updateValueAndValidity();
    }
  }

  onSubmit(): void {
    if (this.form.valid) {
      this.submitForm.emit(this.form.value);
    }
  }

  getExtraFieldControl(key: string): FormControl {
    return this.form.get(key) as FormControl;
  }

  getRendererRegistration(rendererId?: string): FieldRendererRegistration | undefined {
    if (!rendererId) return undefined;
    return this.renderers.find((r) => r.rendererId === rendererId);
  }

  private buildForm(): void {
    this.form = this.fb.group({
      type: ['PF', Validators.required],
      document: ['', [Validators.required, cpfValidator()]],
      displayName: ['', Validators.required],
    });

    // Add extra field controls
    for (const field of this.extraFields) {
      this.form.addControl(
        field.key,
        new FormControl('', field.validatorFns ?? [])
      );
    }

    // Apply host validation rules
    for (const rule of this.validationRules) {
      const control = this.form.get(rule.fieldPath);
      if (control && (!rule.appliesTo || rule.appliesTo.includes(this.currentType))) {
        control.addValidators(rule.validators);
        control.updateValueAndValidity();
      }
    }

    if (this.customer()) {
      this.patchForm();
    }
  }

  private patchForm(): void {
    const c = this.customer();
    if (!c) return;
    this.form.patchValue({
      type: c.type,
      document: c.document,
      displayName: c.displayName,
    });
    this.onTypeChange();

    if (this.editMode()) {
      this.form.get('type')?.disable();
      this.form.get('document')?.disable();
    }
  }
}
