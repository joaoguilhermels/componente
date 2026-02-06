import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { LoyaltyRendererComponent } from './loyalty-renderer.component';
import { FieldRendererContext } from 'customer-registry-ui';

describe('LoyaltyRendererComponent', () => {
  let component: LoyaltyRendererComponent;
  let fixture: ComponentFixture<LoyaltyRendererComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoyaltyRendererComponent, NoopAnimationsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(LoyaltyRendererComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should render a Material input with star icon', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('mat-form-field')).toBeTruthy();
    expect(compiled.querySelector('mat-icon')?.textContent?.trim()).toBe('star');
  });

  it('should display the current value from context', () => {
    component.context = {
      key: 'loyaltyNumber',
      value: 'LOYALTY-1234',
      disabled: false,
      onChange: jest.fn(),
    };
    fixture.detectChanges();

    const input = fixture.nativeElement.querySelector('input') as HTMLInputElement;
    expect(input.value).toBe('LOYALTY-1234');
  });

  it('should call onChange when user types', () => {
    const onChangeSpy = jest.fn();
    component.context = {
      key: 'loyaltyNumber',
      value: '',
      disabled: false,
      onChange: onChangeSpy,
    };
    fixture.detectChanges();

    const input = fixture.nativeElement.querySelector('input') as HTMLInputElement;
    input.value = 'LOYALTY-5678';
    input.dispatchEvent(new Event('input'));

    expect(onChangeSpy).toHaveBeenCalledWith('LOYALTY-5678');
  });

  it('should disable input when context.disabled is true', () => {
    component.context = {
      key: 'loyaltyNumber',
      value: '',
      disabled: true,
      onChange: jest.fn(),
    };
    fixture.detectChanges();

    const input = fixture.nativeElement.querySelector('input') as HTMLInputElement;
    expect(input.disabled).toBe(true);
  });
});
