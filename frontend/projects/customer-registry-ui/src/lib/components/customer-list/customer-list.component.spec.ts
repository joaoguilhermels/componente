import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { CustomerListComponent } from './customer-list.component';
import { Customer } from '../../models/customer.model';
import { CustomerI18nService } from '../../i18n/customer-i18n.service';
import { CUSTOMER_REGISTRY_UI_CONFIG, CUSTOMER_I18N_OVERRIDES } from '../../tokens';
import { DEFAULT_CONFIG } from '../../models/config.model';

describe('CustomerListComponent', () => {
  let component: CustomerListComponent;
  let fixture: ComponentFixture<CustomerListComponent>;

  const mockCustomer: Customer = {
    id: '550e8400-e29b-41d4-a716-446655440000',
    type: 'PF',
    document: '52998224725',
    displayName: 'Maria Silva',
    status: 'ACTIVE',
    addresses: [],
    contacts: [],
    schemaVersion: 1,
    attributes: {},
    createdAt: '2025-01-01T00:00:00Z',
    updatedAt: '2025-01-01T00:00:00Z',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CustomerListComponent, NoopAnimationsModule],
      providers: [
        { provide: CUSTOMER_REGISTRY_UI_CONFIG, useValue: DEFAULT_CONFIG },
        { provide: CUSTOMER_I18N_OVERRIDES, useValue: {} },
        CustomerI18nService,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CustomerListComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => TestBed.resetTestingModule());

  it('should create', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should display customers in the table', () => {
    fixture.componentRef.setInput('customers', [mockCustomer]);
    fixture.componentRef.setInput('totalCount', 1);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('table')).toBeTruthy();
    expect(compiled.textContent).toContain('Maria Silva');
    expect(compiled.textContent).toContain('52998224725');
  });

  it('should show no results message when empty and not loading', () => {
    fixture.componentRef.setInput('customers', []);
    fixture.componentRef.setInput('loading', false);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.crui-no-results')).toBeTruthy();
  });

  it('should show progress bar when loading', () => {
    fixture.componentRef.setInput('loading', true);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('mat-progress-bar')).toBeTruthy();
  });

  it('should emit pageChange on pagination', () => {
    const spy = jest.spyOn(component.pageChange, 'emit');
    const pageEvent = { pageIndex: 1, pageSize: 20, length: 100 };

    component.onPage(pageEvent as any);

    expect(spy).toHaveBeenCalledWith(pageEvent);
  });

  it('should emit sortChange on sort', () => {
    const spy = jest.spyOn(component.sortChange, 'emit');
    const sortEvent = { active: 'displayName', direction: 'asc' as const };

    component.onSort(sortEvent);

    expect(spy).toHaveBeenCalledWith(sortEvent);
  });

  it('should emit selectCustomer on row click', () => {
    const spy = jest.spyOn(component.selectCustomer, 'emit');

    component.onSelect(mockCustomer);

    expect(spy).toHaveBeenCalledWith(mockCustomer);
  });

  it('should use default displayed columns', () => {
    expect(component.displayedColumns()).toEqual([
      'type', 'document', 'displayName', 'status', 'actions',
    ]);
  });

  it('should accept custom displayed columns', () => {
    fixture.componentRef.setInput('displayedColumns', ['document', 'displayName']);
    fixture.detectChanges();
    expect(component.displayedColumns()).toEqual(['document', 'displayName']);
  });
});
