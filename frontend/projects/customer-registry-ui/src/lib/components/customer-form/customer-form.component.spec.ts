import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
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
import { ExtraFieldDefinition } from '../../models/extensibility.model';

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

  function createComponent(extraFields: ExtraFieldDefinition[] = []) {
    const providers: any[] = [
      { provide: CUSTOMER_REGISTRY_UI_CONFIG, useValue: DEFAULT_CONFIG },
      { provide: CUSTOMER_I18N_OVERRIDES, useValue: {} },
      { provide: CUSTOMER_UI_RENDERER_ERROR_REPORTER, useValue: { report: jest.fn() } },
      CustomerI18nService,
    ];

    if (extraFields.length > 0) {
      providers.push({ provide: CUSTOMER_EXTRA_FIELDS, useValue: extraFields, multi: true });
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

    it('should emit cancel', () => {
      const spy = jest.spyOn(component.cancel, 'emit');
      component.cancel.emit();
      expect(spy).toHaveBeenCalled();
    });
  });

  describe('edit mode', () => {
    beforeEach(() => createComponent());

    it('should patch form with customer data', () => {
      component.customer = mockCustomer;
      component.editMode = true;
      component.ngOnChanges({
        customer: {
          currentValue: mockCustomer,
          previousValue: null,
          firstChange: true,
          isFirstChange: () => true,
        },
      });

      expect(component.form.get('type')?.value).toBe('PJ');
      expect(component.form.get('document')?.value).toBe('11222333000181');
      expect(component.form.get('displayName')?.value).toBe('Empresa ABC');
    });

    it('should disable type and document in edit mode', () => {
      component.customer = mockCustomer;
      component.editMode = true;
      component.ngOnChanges({
        customer: {
          currentValue: mockCustomer,
          previousValue: null,
          firstChange: true,
          isFirstChange: () => true,
        },
      });

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
});
