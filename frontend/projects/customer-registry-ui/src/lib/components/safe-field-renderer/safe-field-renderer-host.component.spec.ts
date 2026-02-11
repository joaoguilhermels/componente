import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Component, ErrorHandler } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { SafeFieldRendererHostComponent } from './safe-field-renderer-host.component';
import { FieldRendererRegistration } from '../../models/extensibility.model';
import { CUSTOMER_UI_RENDERER_ERROR_REPORTER } from '../../tokens';
import { CustomerI18nService } from '../../i18n/customer-i18n.service';
import { CUSTOMER_REGISTRY_UI_CONFIG, CUSTOMER_I18N_OVERRIDES } from '../../tokens';
import { DEFAULT_CONFIG } from '../../models/config.model';

/** A simple working renderer */
@Component({
  selector: 'test-good-renderer',
  standalone: true,
  template: '<input class="good-renderer" value="custom" />',
})
class GoodRendererComponent {
  context: unknown;
}

/** A renderer that throws on creation */
@Component({
  selector: 'test-bad-renderer',
  standalone: true,
  template: '<span>bad</span>',
})
class BadRendererComponent {
  constructor() {
    throw new Error('Renderer exploded!');
  }
}

describe('SafeFieldRendererHostComponent', () => {
  let component: SafeFieldRendererHostComponent;
  let fixture: ComponentFixture<SafeFieldRendererHostComponent>;
  let errorReporter: { report: jest.Mock };
  let errorHandler: { handleError: jest.Mock };

  beforeEach(async () => {
    errorReporter = { report: jest.fn() };
    errorHandler = { handleError: jest.fn() };

    await TestBed.configureTestingModule({
      imports: [
        SafeFieldRendererHostComponent,
        NoopAnimationsModule,
        ReactiveFormsModule,
      ],
      providers: [
        { provide: CUSTOMER_UI_RENDERER_ERROR_REPORTER, useValue: errorReporter },
        { provide: ErrorHandler, useValue: errorHandler },
        { provide: CUSTOMER_REGISTRY_UI_CONFIG, useValue: DEFAULT_CONFIG },
        { provide: CUSTOMER_I18N_OVERRIDES, useValue: {} },
        CustomerI18nService,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SafeFieldRendererHostComponent);
    component = fixture.componentInstance;
    component.control = new FormControl('test-value');
    component.labelKey = 'field.document';
  });

  afterEach(() => TestBed.resetTestingModule());

  it('should create', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should not use fallback initially', () => {
    fixture.detectChanges();
    expect(component.useFallback()).toBe(false);
  });

  describe('with a working renderer', () => {
    beforeEach(() => {
      const registration: FieldRendererRegistration = {
        rendererId: 'good-renderer',
        component: GoodRendererComponent,
      };
      component.registration = registration;
    });

    it('should render the custom component', () => {
      fixture.detectChanges();
      // Trigger ngOnChanges manually since we set input after creation
      component.ngOnChanges({
        registration: {
          currentValue: component.registration,
          previousValue: undefined,
          firstChange: true,
          isFirstChange: () => true,
        },
      });
      fixture.detectChanges();

      expect(component.useFallback()).toBe(false);
      expect(errorReporter.report).not.toHaveBeenCalled();
    });
  });

  describe('with a buggy renderer', () => {
    beforeEach(() => {
      const registration: FieldRendererRegistration = {
        rendererId: 'bad-renderer',
        component: BadRendererComponent,
      };
      component.registration = registration;
    });

    it('should activate fallback on renderer failure', () => {
      fixture.detectChanges();
      component.ngOnChanges({
        registration: {
          currentValue: component.registration,
          previousValue: undefined,
          firstChange: true,
          isFirstChange: () => true,
        },
      });
      fixture.detectChanges();

      expect(component.useFallback()).toBe(true);
    });

    it('should report error to error reporter', () => {
      fixture.detectChanges();
      component.ngOnChanges({
        registration: {
          currentValue: component.registration,
          previousValue: undefined,
          firstChange: true,
          isFirstChange: () => true,
        },
      });
      fixture.detectChanges();

      expect(errorReporter.report).toHaveBeenCalledWith(
        'bad-renderer',
        expect.any(Error)
      );
    });

    it('should call ErrorHandler.handleError', () => {
      fixture.detectChanges();
      component.ngOnChanges({
        registration: {
          currentValue: component.registration,
          previousValue: undefined,
          firstChange: true,
          isFirstChange: () => true,
        },
      });
      fixture.detectChanges();

      expect(errorHandler.handleError).toHaveBeenCalledWith(
        expect.any(Error)
      );
    });

    it('should render fallback input when in fallback mode', () => {
      fixture.detectChanges();
      component.ngOnChanges({
        registration: {
          currentValue: component.registration,
          previousValue: undefined,
          firstChange: true,
          isFirstChange: () => true,
        },
      });
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('mat-form-field')).toBeTruthy();
    });
  });

  describe('bidirectional fallback sync (A1)', () => {
    beforeEach(() => {
      const registration: FieldRendererRegistration = {
        rendererId: 'bad-renderer',
        component: BadRendererComponent,
      };
      component.registration = registration;
      fixture.detectChanges();
      component.ngOnChanges({
        registration: {
          currentValue: registration,
          previousValue: undefined,
          firstChange: true,
          isFirstChange: () => true,
        },
      });
      fixture.detectChanges();
    });

    it('should sync external control updates to fallback', () => {
      expect(component.useFallback()).toBe(true);

      component.control.setValue('updated-externally');

      expect(component.fallbackControl.value).toBe('updated-externally');
    });

    it('should sync fallback updates to the external control', () => {
      expect(component.useFallback()).toBe(true);

      component.fallbackControl.setValue('from-fallback');

      expect(component.control.value).toBe('from-fallback');
    });

    it('should not cause infinite loop between synced controls', () => {
      expect(component.useFallback()).toBe(true);

      const controlSetSpy = jest.spyOn(component.control, 'setValue');
      const fallbackSetSpy = jest.spyOn(component.fallbackControl, 'setValue');

      component.control.setValue('trigger');

      // control.setValue called once (by test), fallback.setValue called once (by subscription with emitEvent:false)
      // The emitEvent:false prevents the fallback change from re-triggering control.setValue
      expect(controlSetSpy).toHaveBeenCalledTimes(1);
      expect(fallbackSetSpy).toHaveBeenCalledTimes(1);
    });
  });

  describe('context injection error fallback (B5)', () => {
    it('should activate fallback when context assignment throws', () => {
      @Component({
        selector: 'test-readonly-context-renderer',
        standalone: true,
        template: '<span>readonly</span>',
      })
      class ReadonlyContextRendererComponent {
        get context(): unknown {
          return {};
        }
        set context(_: unknown) {
          throw new Error('Cannot set context');
        }
      }

      const registration: FieldRendererRegistration = {
        rendererId: 'readonly-context',
        component: ReadonlyContextRendererComponent,
      };
      component.registration = registration;
      fixture.detectChanges();
      component.ngOnChanges({
        registration: {
          currentValue: registration,
          previousValue: undefined,
          firstChange: true,
          isFirstChange: () => true,
        },
      });
      fixture.detectChanges();

      expect(component.useFallback()).toBe(true);
      expect(errorReporter.report).toHaveBeenCalledWith(
        'readonly-context',
        expect.any(Error),
      );
    });
  });

  describe('missing registration (C11)', () => {
    it('should report error when no registration is provided', () => {
      component.fieldKey = 'test-field';
      // registration is not set (undefined)
      fixture.detectChanges();
      component.ngOnChanges({
        registration: {
          currentValue: undefined,
          previousValue: undefined,
          firstChange: true,
          isFirstChange: () => true,
        },
      });
      fixture.detectChanges();

      expect(errorReporter.report).toHaveBeenCalledWith(
        'test-field',
        expect.any(Error),
      );
      expect(component.useFallback()).toBe(true);
    });

    it('should use "unknown" as identifier when fieldKey is empty', () => {
      component.fieldKey = '';
      fixture.detectChanges();
      component.ngOnChanges({
        registration: {
          currentValue: undefined,
          previousValue: undefined,
          firstChange: true,
          isFirstChange: () => true,
        },
      });
      fixture.detectChanges();

      expect(errorReporter.report).toHaveBeenCalledWith(
        'unknown',
        expect.any(Error),
      );
    });
  });
});
