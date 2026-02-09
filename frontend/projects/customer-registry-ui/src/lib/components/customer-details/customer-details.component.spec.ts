import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { CustomerDetailsComponent } from './customer-details.component';
import { Customer } from '../../models/customer.model';
import { CustomerI18nService } from '../../i18n/customer-i18n.service';
import { CUSTOMER_REGISTRY_UI_CONFIG, CUSTOMER_I18N_OVERRIDES } from '../../tokens';
import { DEFAULT_CONFIG } from '../../models/config.model';

describe('CustomerDetailsComponent', () => {
  let component: CustomerDetailsComponent;
  let fixture: ComponentFixture<CustomerDetailsComponent>;

  const mockCustomer: Customer = {
    id: '550e8400-e29b-41d4-a716-446655440000',
    type: 'PF',
    document: '52998224725',
    displayName: 'Maria Silva',
    status: 'ACTIVE',
    addresses: [
      {
        id: 'addr-1',
        street: 'Rua das Flores',
        number: '123',
        complement: 'Apto 4',
        neighborhood: 'Centro',
        city: 'São Paulo',
        state: 'SP',
        zipCode: '01001-000',
        country: 'BR',
      },
    ],
    contacts: [
      { id: 'ct-1', type: 'EMAIL', value: 'maria@example.com', primary: true },
      { id: 'ct-2', type: 'PHONE', value: '+5511999998888', primary: false },
    ],
    schemaVersion: 1,
    attributes: {},
    createdAt: '2025-01-01T00:00:00Z',
    updatedAt: '2025-06-15T10:30:00Z',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CustomerDetailsComponent, NoopAnimationsModule],
      providers: [
        { provide: CUSTOMER_REGISTRY_UI_CONFIG, useValue: DEFAULT_CONFIG },
        { provide: CUSTOMER_I18N_OVERRIDES, useValue: {} },
        CustomerI18nService,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CustomerDetailsComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => TestBed.resetTestingModule());

  it('should create', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should render nothing when customer is null', () => {
    component.customer = null;
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('mat-card')).toBeFalsy();
  });

  it('should display customer details', () => {
    component.customer = mockCustomer;
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Maria Silva');
    expect(compiled.textContent).toContain('52998224725');
  });

  it('should display addresses', () => {
    component.customer = mockCustomer;
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Rua das Flores');
    expect(compiled.textContent).toContain('123');
    expect(compiled.textContent).toContain('Apto 4');
  });

  it('should display contacts', () => {
    component.customer = mockCustomer;
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('maria@example.com');
    expect(compiled.textContent).toContain('+5511999998888');
  });

  it('should emit edit event when edit button is clicked', () => {
    const spy = jest.spyOn(component.edit, 'emit');
    component.customer = mockCustomer;
    fixture.detectChanges();

    const buttons = fixture.nativeElement.querySelectorAll('button');
    const editButton = Array.from(buttons).find(
      (btn: any) => btn.textContent?.includes('edit')
    ) as HTMLButtonElement;
    editButton.click();

    expect(spy).toHaveBeenCalledWith(mockCustomer);
  });

  it('should emit back event when back button is clicked', () => {
    const spy = jest.spyOn(component.back, 'emit');
    component.customer = mockCustomer;
    fixture.detectChanges();

    const buttons = fixture.nativeElement.querySelectorAll('button');
    // The back button text is translated: "Voltar" (pt-BR) or "Back" (en)
    const backButton = Array.from(buttons).find(
      (btn: any) =>
        btn.textContent?.includes('Voltar') || btn.textContent?.includes('Back')
    ) as HTMLButtonElement;
    backButton.click();

    expect(spy).toHaveBeenCalled();
  });
});
