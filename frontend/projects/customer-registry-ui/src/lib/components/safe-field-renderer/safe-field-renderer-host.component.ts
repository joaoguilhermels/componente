import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ComponentRef,
  ErrorHandler,
  inject,
  Input,
  OnChanges,
  OnDestroy,
  signal,
  SimpleChanges,
  ViewChild,
  ViewContainerRef,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormControl } from '@angular/forms';
import { Subscription } from 'rxjs';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { TranslatePipe } from '../../i18n/translate.pipe';
import {
  FieldRendererContext,
  FieldRendererRegistration,
} from '../../models/extensibility.model';
import { CUSTOMER_UI_RENDERER_ERROR_REPORTER } from '../../tokens';

/**
 * Hosts a custom field renderer with graceful degradation.
 *
 * If the custom renderer throws during creation or change detection,
 * this component falls back to a standard Material text input and
 * reports the error via the CUSTOMER_UI_RENDERER_ERROR_REPORTER token.
 */
@Component({
  selector: 'crui-safe-field-renderer',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    TranslatePipe,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (!useFallback()) {
      <ng-container #rendererHost></ng-container>
    }
    @if (useFallback()) {
      <mat-form-field appearance="outline" class="crui-fallback-field">
        <mat-label>{{ labelKey | translate }}</mat-label>
        <input matInput [formControl]="fallbackControl" />
      </mat-form-field>
    }
  `,
  styles: [`
    .crui-fallback-field {
      width: 100%;
    }
  `],
})
export class SafeFieldRendererHostComponent implements OnChanges, AfterViewInit, OnDestroy {
  /** The renderer registration to use for this field */
  @Input() registration!: FieldRendererRegistration;

  /** i18n key for the field label */
  @Input() labelKey = '';

  /** The form control to bind the field to */
  @Input() control!: FormControl;

  /** Whether the field should be disabled */
  @Input() disabled = false;

  /** Identifier for this field in the renderer context (used as context key) */
  @Input() fieldKey = '';

  @ViewChild('rendererHost', { read: ViewContainerRef, static: false })
  rendererHost?: ViewContainerRef;

  readonly useFallback = signal(false);
  readonly fallbackControl = new FormControl('');

  private readonly errorReporter = inject(CUSTOMER_UI_RENDERER_ERROR_REPORTER);
  private readonly errorHandler = inject(ErrorHandler);
  private componentRef?: ComponentRef<unknown>;
  private fallbackSubscription?: Subscription;
  private controlSubscription?: Subscription;
  private pendingRetry = false;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['registration'] || changes['control']) {
      if (this.rendererHost) {
        this.tryCreateRenderer();
      } else {
        this.pendingRetry = true;
      }
    }
    if (changes['disabled']) {
      if (this.disabled) {
        this.fallbackControl.disable();
      } else {
        this.fallbackControl.enable();
      }
      if (this.componentRef) {
        const instance = this.componentRef.instance as Record<string, unknown>;
        if ('setDisabledState' in instance && typeof instance['setDisabledState'] === 'function') {
          (instance['setDisabledState'] as (disabled: boolean) => void)(this.disabled);
        }
      }
    }
  }

  ngAfterViewInit(): void {
    if (this.pendingRetry) {
      this.pendingRetry = false;
      this.tryCreateRenderer();
    }
  }

  ngOnDestroy(): void {
    this.fallbackSubscription?.unsubscribe();
    this.controlSubscription?.unsubscribe();
    this.rendererHost?.clear();
  }

  private tryCreateRenderer(): void {
    // Reset fallback state
    this.useFallback.set(false);

    if (!this.registration || !this.rendererHost) {
      this.errorReporter.report(
        this.fieldKey || 'unknown',
        new Error('No renderer registration found'),
      );
      this.activateFallback('no-registration');
      return;
    }

    try {
      this.rendererHost.clear();
      this.componentRef = undefined;

      const componentRef = this.rendererHost.createComponent(
        this.registration.component
      );
      this.componentRef = componentRef;

      // Inject context into the renderer
      const context: FieldRendererContext = {
        key: this.fieldKey,
        value: this.control?.value,
        disabled: this.disabled,
        onChange: (value: unknown) => {
          this.control?.setValue(value);
        },
      };

      // Try to set context if the component has an input for it
      const instance = componentRef.instance as Record<string, unknown>;
      if ('context' in instance) {
        try {
          instance['context'] = context;
        } catch (ctxError) {
          this.errorHandler.handleError(ctxError);
          this.activateFallback(this.registration.rendererId, ctxError);
          return;
        }
      }

      componentRef.changeDetectorRef.detectChanges();
    } catch (error) {
      this.errorHandler.handleError(error);
      this.activateFallback(this.registration.rendererId, error);
    }
  }

  private activateFallback(rendererId: string, error?: unknown): void {
    this.useFallback.set(true);
    this.fallbackControl.setValue(this.control?.value ?? '');

    // Unsubscribe previous fallback sync before creating a new one
    this.fallbackSubscription?.unsubscribe();

    // Sync fallback control -> form control (with emitEvent: false to prevent loops)
    this.fallbackSubscription = this.fallbackControl.valueChanges.subscribe((value) => {
      this.control?.setValue(value, { emitEvent: false });
    });

    // Sync form control -> fallback control (reverse direction)
    this.controlSubscription?.unsubscribe();
    this.controlSubscription = this.control?.valueChanges.subscribe((value) => {
      this.fallbackControl.setValue(value, { emitEvent: false });
    });

    if (error) {
      this.errorReporter.report(rendererId, error);
    }
  }
}
