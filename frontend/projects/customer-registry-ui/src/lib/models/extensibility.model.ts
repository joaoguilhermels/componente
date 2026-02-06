import { Type } from '@angular/core';
import { ValidatorFn } from '@angular/forms';
import { CustomerType } from './customer.model';

/**
 * Definition for a custom field that host apps can inject
 * into the customer form via the CUSTOMER_EXTRA_FIELDS token.
 */
export interface ExtraFieldDefinition {
  /** Unique key used as the form control name */
  key: string;
  /** i18n key for the field label */
  labelKey: string;
  /** HTML input type (text, number, date, etc.) or 'custom' for a custom renderer */
  type: 'text' | 'number' | 'date' | 'select' | 'custom';
  /** Which customer types this field applies to (omit for both) */
  appliesTo?: CustomerType[];
  /** Angular form validators */
  validatorFns?: ValidatorFn[];
  /** ID of a registered custom renderer (when type is 'custom') */
  rendererId?: string;
  /** Options for select fields */
  options?: { value: string; labelKey: string }[];
}

/**
 * Registration of a custom renderer component for extra fields.
 */
export interface FieldRendererRegistration {
  /** Unique renderer ID, referenced by ExtraFieldDefinition.rendererId */
  rendererId: string;
  /** The Angular component class to render */
  component: Type<unknown>;
}

/**
 * Context passed to custom field renderer components via injection.
 */
export interface FieldRendererContext {
  /** The form control name/key */
  key: string;
  /** Current form control value */
  value: unknown;
  /** Whether the field is disabled */
  disabled: boolean;
  /** Callback to emit value changes */
  onChange: (value: unknown) => void;
}

/**
 * Custom validation rule that host apps can inject
 * via the CUSTOMER_VALIDATION_RULES token.
 */
export interface CustomerValidationRule {
  /** Dot-path to the form field (e.g., 'displayName', 'addresses.0.zipCode') */
  fieldPath: string;
  /** Which customer types this rule applies to (omit for both) */
  appliesTo?: CustomerType[];
  /** Angular validators to apply */
  validators: ValidatorFn[];
}

/**
 * Error reporter interface for graceful degradation of custom renderers.
 */
export interface RendererErrorReporter {
  report(rendererId: string, error: unknown): void;
}
