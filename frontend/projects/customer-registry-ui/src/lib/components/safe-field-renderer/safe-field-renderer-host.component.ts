import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
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
export class SafeFieldRendererHostComponent implements OnChanges, OnDestroy {
  @Input() registration!: FieldRendererRegistration;
  @Input() labelKey = '';
  @Input() control!: FormControl;
  @Input() disabled = false;

  @ViewChild('rendererHost', { read: ViewContainerRef, static: false })
  rendererHost?: ViewContainerRef;

  readonly useFallback = signal(false);
  readonly fallbackControl = new FormControl('');

  private readonly errorReporter = inject(CUSTOMER_UI_RENDERER_ERROR_REPORTER);
  private readonly errorHandler = inject(ErrorHandler);
  private readonly cdr = inject(ChangeDetectorRef);

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['registration'] || changes['control']) {
      this.tryCreateRenderer();
    }
    if (changes['disabled']) {
      if (this.disabled) {
        this.fallbackControl.disable();
      } else {
        this.fallbackControl.enable();
      }
    }
  }

  ngOnDestroy(): void {
    this.rendererHost?.clear();
  }

  private tryCreateRenderer(): void {
    // Reset fallback state
    this.useFallback.set(false);
    this.cdr.detectChanges();

    if (!this.registration || !this.rendererHost) {
      this.activateFallback('no-registration');
      return;
    }

    try {
      this.rendererHost.clear();

      const componentRef = this.rendererHost.createComponent(
        this.registration.component
      );

      // Inject context into the renderer
      const context: FieldRendererContext = {
        key: this.control?.value ?? '',
        value: this.control?.value,
        disabled: this.disabled,
        onChange: (value: unknown) => {
          this.control?.setValue(value);
        },
      };

      // Try to set context if the component has an input for it
      const instance = componentRef.instance as Record<string, unknown>;
      if ('context' in instance) {
        instance['context'] = context;
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

    // Sync fallback control with the form control
    this.fallbackControl.valueChanges.subscribe((value) => {
      this.control?.setValue(value);
    });

    if (error) {
      this.errorReporter.report(rendererId, error);
    }
    this.cdr.detectChanges();
  }
}
