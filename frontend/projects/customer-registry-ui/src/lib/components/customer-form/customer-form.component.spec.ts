import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Validators } from '@angular/forms';
import { CustomerFormComponent } from './customer-form.component';
import {
  CUSTOMER_EXTRA_FIELDS,
  CUSTOMER_FIELD_RENDERERS,
  CUSTOMER_I18N_OVERRIDES,
  CUSTOMER_REGISTRY_UI_CONFIG,
  CUSTOMER_UI_RENDERER_ERROR_REPORTER,
  CUSTOMER_VALIDATION_RULES,
} from '../../tokens';
import { DEFAULT_CONFIG } from '../../models/config.model';
import { CustomerI18nService } from '../../i18n/customer-i18n.service';
import { Customer, CreateCustomerRequest } from '../../models/customer.model';
import { ExtraFieldDefinition, CustomerValidationRule } from '../../models/extensibility.model';

describe('CustomerFormComponent', () => {
  let component: CustomerFormComponent;
  let fixture: ComponentFixture<CustomerFormComponent>;

  const mockCustomer: Customer = {
    id: '550e8400-e29b-41d4-a716-446655440000',
    type: 'PJ',
    document: '11222333000181',
    displayName: 'Empresa ABC',
    status: 'ACTIVE',
    addresses: [],
    contacts: [],
    schemaVersion: 1,
    attributes: {},
    createdAt: '2025-01-01T00:00:00Z',
    updatedAt: '2025-01-01T00:00:00Z',
  };

  function createComponent(
    extraFields: ExtraFieldDefinition[] = [],
    validationRules: CustomerValidationRule[] = [],
  ) {
    const providers: any[] = [
      { provide: CUSTOMER_REGISTRY_UI_CONFIG, useValue: DEFAULT_CONFIG },
      { provide: CUSTOMER_I18N_OVERRIDES, useValue: {} },
      { provide: CUSTOMER_UI_RENDERER_ERROR_REPORTER, useValue: { report: jest.fn() } },
      CustomerI18nService,
    ];

    if (extraFields.length > 0) {
      providers.push({ provide: CUSTOMER_EXTRA_FIELDS, useValue: extraFields, multi: true });
    }

    if (validationRules.length > 0) {
      providers.push({ provide: CUSTOMER_VALIDATION_RULES, useValue: validationRules, multi: true });
    }

    TestBed.configureTestingModule({
      imports: [CustomerFormComponent, NoopAnimationsModule],
      providers,
    });

    fixture = TestBed.createComponent(CustomerFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  afterEach(() => TestBed.resetTestingModule());

  describe('creation', () => {
    beforeEach(() => createComponent());

    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should initialize form with PF type', () => {
      expect(component.form.get('type')?.value).toBe('PF');
    });

    it('should have required validators on core fields', () => {
      expect(component.form.get('document')?.hasError('required')).toBe(true);
      expect(component.form.get('displayName')?.hasError('required')).toBe(true);
    });

    it('should use CPF validator for PF type by default', () => {
      component.form.get('document')?.setValue('invalid');
      expect(component.form.get('document')?.hasError('cpfInvalid')).toBe(true);
    });
  });

  describe('type switching', () => {
    beforeEach(() => createComponent());

    it('should switch to CNPJ validator when type changes to PJ', () => {
      component.form.get('type')?.setValue('PJ');
      component.onTypeChange();

      component.form.get('document')?.setValue('invalid');
      expect(component.form.get('document')?.hasError('cnpjInvalid')).toBe(true);
      expect(component.form.get('document')?.hasError('cpfInvalid')).toBeFalsy();
    });

    it('should switch back to CPF validator when type changes to PF', () => {
      component.form.get('type')?.setValue('PJ');
      component.onTypeChange();

      component.form.get('type')?.setValue('PF');
      component.onTypeChange();

      component.form.get('document')?.setValue('invalid');
      expect(component.form.get('document')?.hasError('cpfInvalid')).toBe(true);
    });
  });

  describe('form submission', () => {
    beforeEach(() => createComponent());

    it('should emit submitForm with form values', () => {
      const spy = jest.spyOn(component.submitForm, 'emit');

      component.form.get('type')?.setValue('PF');
      component.form.get('document')?.setValue('52998224725');
      component.form.get('displayName')?.setValue('Maria Silva');
      component.onTypeChange();

      component.onSubmit();

      expect(spy).toHaveBeenCalledWith(
        expect.objectContaining({
          type: 'PF',
          document: '52998224725',
          displayName: 'Maria Silva',
        })
      );
    });

    it('should not emit when form is invalid', () => {
      const spy = jest.spyOn(component.submitForm, 'emit');

      component.form.get('document')?.setValue('');
      component.onSubmit();

      expect(spy).not.toHaveBeenCalled();
    });

    it('should emit cancel when cancel button is clicked', () => {
      const spy = jest.spyOn(component.cancel, 'emit');

      const buttons = fixture.nativeElement.querySelectorAll('button');
      const cancelButton = Array.from(buttons).find(
        (btn: any) =>
          btn.textContent?.includes('cancel') || btn.textContent?.includes('Cancel')
      ) as HTMLButtonElement;
      cancelButton.click();

      expect(spy).toHaveBeenCalled();
    });
  });

  describe('edit mode', () => {
    beforeEach(() => createComponent());

    it('should patch form with customer data', () => {
      fixture.componentRef.setInput('editMode', true);
      fixture.componentRef.setInput('customer', mockCustomer);
      fixture.detectChanges();

      expect(component.form.get('type')?.value).toBe('PJ');
      expect(component.form.get('document')?.value).toBe('11222333000181');
      expect(component.form.get('displayName')?.value).toBe('Empresa ABC');
    });

    it('should disable type and document in edit mode', () => {
      fixture.componentRef.setInput('editMode', true);
      fixture.componentRef.setInput('customer', mockCustomer);
      fixture.detectChanges();

      expect(component.form.get('type')?.disabled).toBe(true);
      expect(component.form.get('document')?.disabled).toBe(true);
    });
  });

  describe('extra fields', () => {
    it('should add extra field controls to the form', () => {
      const extraFields: ExtraFieldDefinition[] = [
        { key: 'loyalty', labelKey: 'field.loyalty', type: 'text' },
        { key: 'notes', labelKey: 'field.notes', type: 'text' },
      ];
      createComponent(extraFields);

      expect(component.form.get('loyalty')).toBeTruthy();
      expect(component.form.get('notes')).toBeTruthy();
    });

    it('should filter extra fields by customer type', () => {
      const extraFields: ExtraFieldDefinition[] = [
        { key: 'cpfField', labelKey: 'field.cpfField', type: 'text', appliesTo: ['PF'] },
        { key: 'cnpjField', labelKey: 'field.cnpjField', type: 'text', appliesTo: ['PJ'] },
        { key: 'bothField', labelKey: 'field.bothField', type: 'text' },
      ];
      createComponent(extraFields);

      // Default type is PF
      const visible = component.visibleExtraFields;
      expect(visible.map((f) => f.key)).toContain('cpfField');
      expect(visible.map((f) => f.key)).not.toContain('cnpjField');
      expect(visible.map((f) => f.key)).toContain('bothField');
    });
  });

  describe('validation rules (C8)', () => {
    it('should apply host validation rules to matching fields', () => {
      const rules: CustomerValidationRule[] = [
        { fieldPath: 'displayName', validators: [Validators.minLength(3)] },
      ];
      createComponent([], rules);

      component.form.get('displayName')?.setValue('AB');
      expect(component.form.get('displayName')?.hasError('minlength')).toBe(true);

      component.form.get('displayName')?.setValue('ABC');
      expect(component.form.get('displayName')?.hasError('minlength')).toBe(false);
    });

    it('should apply type-specific rules only to matching type', () => {
      const rules: CustomerValidationRule[] = [
        { fieldPath: 'displayName', appliesTo: ['PJ'], validators: [Validators.minLength(5)] },
      ];
      createComponent([], rules);

      // Default type is PF, rule should not be applied
      component.form.get('displayName')?.setValue('AB');
      expect(component.form.get('displayName')?.hasError('minlength')).toBe(false);
    });

    it('should not fail for rules targeting non-existent field paths', () => {
      const rules: CustomerValidationRule[] = [
        { fieldPath: 'nonExistentField', validators: [Validators.required] },
      ];
      expect(() => createComponent([], rules)).not.toThrow();
    });

    it('should apply multiple rules to the same field', () => {
      const rules: CustomerValidationRule[] = [
        { fieldPath: 'displayName', validators: [Validators.minLength(3)] },
        { fieldPath: 'displayName', validators: [Validators.maxLength(50)] },
      ];
      createComponent([], rules);

      component.form.get('displayName')?.setValue('AB');
      expect(component.form.get('displayName')?.hasError('minlength')).toBe(true);

      component.form.get('displayName')?.setValue('A'.repeat(51));
      expect(component.form.get('displayName')?.hasError('maxlength')).toBe(true);

      component.form.get('displayName')?.setValue('Valid Name');
      expect(component.form.get('displayName')?.valid).toBe(true);
    });
  });

  describe('extra field validators on type change (A2)', () => {
    it('should clear PJ-only field validators when type switches to PF', () => {
      const extraFields: ExtraFieldDefinition[] = [
        {
          key: 'companySize',
          labelKey: 'field.companySize',
          type: 'text',
          appliesTo: ['PJ'],
          validatorFns: [Validators.required],
        },
      ];
      createComponent(extraFields);

      // Switch to PJ so companySize is visible and has validators
      component.form.get('type')?.setValue('PJ');
      component.onTypeChange();
      component.form.get('companySize')?.setValue('');
      expect(component.form.get('companySize')?.hasError('required')).toBe(true);

      // Switch back to PF — validators should be cleared
      component.form.get('type')?.setValue('PF');
      component.onTypeChange();
      component.form.get('companySize')?.setValue('');
      component.form.get('companySize')?.updateValueAndValidity();
      expect(component.form.get('companySize')?.hasError('required')).toBe(false);
    });

    it('should restore PF-only field validators when type switches back to PF', () => {
      const extraFields: ExtraFieldDefinition[] = [
        {
          key: 'nickname',
          labelKey: 'field.nickname',
          type: 'text',
          appliesTo: ['PF'],
          validatorFns: [Validators.required],
        },
      ];
      createComponent(extraFields);

      // Default is PF: field should have required
      component.form.get('nickname')?.setValue('');
      expect(component.form.get('nickname')?.hasError('required')).toBe(true);

      // Switch to PJ — validators should be cleared
      component.form.get('type')?.setValue('PJ');
      component.onTypeChange();
      component.form.get('nickname')?.setValue('');
      component.form.get('nickname')?.updateValueAndValidity();
      expect(component.form.get('nickname')?.hasError('required')).toBe(false);

      // Switch back to PF — validators should be restored
      component.form.get('type')?.setValue('PF');
      component.onTypeChange();
      component.form.get('nickname')?.setValue('');
      component.form.get('nickname')?.updateValueAndValidity();
      expect(component.form.get('nickname')?.hasError('required')).toBe(true);
    });

    it('should always have validators on fields without appliesTo', () => {
      const extraFields: ExtraFieldDefinition[] = [
        {
          key: 'universalRequired',
          labelKey: 'field.universal',
          type: 'text',
          validatorFns: [Validators.required],
        },
      ];
      createComponent(extraFields);

      // PF (default)
      component.form.get('universalRequired')?.setValue('');
      expect(component.form.get('universalRequired')?.hasError('required')).toBe(true);

      // Switch to PJ — validators should remain
      component.form.get('type')?.setValue('PJ');
      component.onTypeChange();
      component.form.get('universalRequired')?.setValue('');
      component.form.get('universalRequired')?.updateValueAndValidity();
      expect(component.form.get('universalRequired')?.hasError('required')).toBe(true);

      // Switch back to PF — validators still remain
      component.form.get('type')?.setValue('PF');
      component.onTypeChange();
      component.form.get('universalRequired')?.setValue('');
      component.form.get('universalRequired')?.updateValueAndValidity();
      expect(component.form.get('universalRequired')?.hasError('required')).toBe(true);
    });

    it('should clear/restore host validation rules on type change', () => {
      const rules: CustomerValidationRule[] = [
        { fieldPath: 'displayName', appliesTo: ['PJ'], validators: [Validators.minLength(5)] },
      ];
      createComponent([], rules);

      // Default type is PF — PJ-only rule should not be active
      component.form.get('displayName')?.setValue('AB');
      expect(component.form.get('displayName')?.hasError('minlength')).toBe(false);

      // Switch to PJ — PJ-only rule should be active
      component.form.get('type')?.setValue('PJ');
      component.onTypeChange();
      component.form.get('displayName')?.setValue('AB');
      component.form.get('displayName')?.updateValueAndValidity();
      expect(component.form.get('displayName')?.hasError('minlength')).toBe(true);

      // Switch back to PF — PJ-only rule should be cleared again
      component.form.get('type')?.setValue('PF');
      component.onTypeChange();
      component.form.get('displayName')?.setValue('AB');
      component.form.get('displayName')?.updateValueAndValidity();
      expect(component.form.get('displayName')?.hasError('minlength')).toBe(false);
    });
  });

  describe('appliesTo edge cases (C10)', () => {
    it('should show extra fields with empty appliesTo array for all types', () => {
      const extraFields: ExtraFieldDefinition[] = [
        { key: 'globalField', labelKey: 'field.global', type: 'text', appliesTo: [] },
      ];
      createComponent(extraFields);

      // appliesTo is empty array (truthy but length 0) — should still be visible because
      // the filter is: !f.appliesTo || f.appliesTo.includes(...)
      // Empty array is truthy, and [].includes('PF') is false => field is hidden
      const visible = component.visibleExtraFields;
      expect(visible.map((f) => f.key)).not.toContain('globalField');
    });

    it('should show extra fields without appliesTo for any type', () => {
      const extraFields: ExtraFieldDefinition[] = [
        { key: 'universalField', labelKey: 'field.universal', type: 'text' },
      ];
      createComponent(extraFields);

      const visible = component.visibleExtraFields;
      expect(visible.map((f) => f.key)).toContain('universalField');
    });

    it('should hide PJ-only extra fields when type is PF', () => {
      const extraFields: ExtraFieldDefinition[] = [
        { key: 'pjOnly', labelKey: 'field.pjOnly', type: 'text', appliesTo: ['PJ'] },
      ];
      createComponent(extraFields);

      expect(component.currentType).toBe('PF');
      expect(component.visibleExtraFields.map((f) => f.key)).not.toContain('pjOnly');
    });

    it('should show PJ-only extra fields after switching to PJ', () => {
      const extraFields: ExtraFieldDefinition[] = [
        { key: 'pjOnly', labelKey: 'field.pjOnly', type: 'text', appliesTo: ['PJ'] },
      ];
      createComponent(extraFields);

      component.form.get('type')?.setValue('PJ');
      component.onTypeChange();

      expect(component.visibleExtraFields.map((f) => f.key)).toContain('pjOnly');
    });
  });
});
