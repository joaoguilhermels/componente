import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { CustomerSearchComponent } from './customer-search.component';
import { CUSTOMER_REGISTRY_UI_CONFIG } from '../../tokens';
import { DEFAULT_CONFIG } from '../../models/config.model';
import { CustomerI18nService } from '../../i18n/customer-i18n.service';
import { CUSTOMER_I18N_OVERRIDES } from '../../tokens';

describe('CustomerSearchComponent', () => {
  let component: CustomerSearchComponent;
  let fixture: ComponentFixture<CustomerSearchComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CustomerSearchComponent, NoopAnimationsModule],
      providers: [
        { provide: CUSTOMER_REGISTRY_UI_CONFIG, useValue: DEFAULT_CONFIG },
        { provide: CUSTOMER_I18N_OVERRIDES, useValue: {} },
        CustomerI18nService,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CustomerSearchComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => TestBed.resetTestingModule());

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should emit search params on search', () => {
    const spy = jest.spyOn(component.search, 'emit');

    component.searchType = 'PF';
    component.searchDocument = '12345678901';
    component.onSearch();

    expect(spy).toHaveBeenCalledWith({
      type: 'PF',
      document: '12345678901',
    });
  });

  it('should omit empty fields from search params', () => {
    const spy = jest.spyOn(component.search, 'emit');

    component.searchType = undefined;
    component.searchDocument = '';
    component.searchDisplayName = '  ';
    component.onSearch();

    expect(spy).toHaveBeenCalledWith({});
  });

  it('should reset all fields on reset', () => {
    const resetSpy = jest.spyOn(component.reset, 'emit');

    component.searchType = 'PJ';
    component.searchDocument = '123';
    component.searchDisplayName = 'Test';
    component.searchStatus = 'ACTIVE';

    component.onReset();

    expect(component.searchType).toBeUndefined();
    expect(component.searchDocument).toBe('');
    expect(component.searchDisplayName).toBe('');
    expect(component.searchStatus).toBeUndefined();
    expect(resetSpy).toHaveBeenCalled();
  });

  it('should be visible when search feature is enabled', () => {
    expect(component.isVisible).toBe(true);
  });

  it('should not be visible when search feature is disabled', () => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [CustomerSearchComponent, NoopAnimationsModule],
      providers: [
        {
          provide: CUSTOMER_REGISTRY_UI_CONFIG,
          useValue: {
            ...DEFAULT_CONFIG,
            features: { ...DEFAULT_CONFIG.features, search: false },
          },
        },
        { provide: CUSTOMER_I18N_OVERRIDES, useValue: {} },
        CustomerI18nService,
      ],
    });
    const fix = TestBed.createComponent(CustomerSearchComponent);
    const comp = fix.componentInstance;
    expect(comp.isVisible).toBe(false);
  });
});
