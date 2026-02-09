---
applyTo: "frontend/**/*.ts"
---

# Angular Code Rules

The frontend follows a library-first pattern using Angular 17 with standalone
components and signal-based state management.

## Component Pattern

All components must be standalone with `OnPush` change detection:

```typescript
@Component({
  selector: '<ANGULAR_PREFIX>-customer-list',
  standalone: true,
  imports: [CommonModule, MatTableModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './customer-list.component.html',
})
export class CustomerListComponent { }
```

## Signal-Based State Management

Use `WritableSignal` for private state and `asReadonly()` for public API:

```typescript
@Injectable({ providedIn: 'root' })
export class CustomerStateService {
  private readonly _customers = signal<Customer[]>([]);
  private readonly _loading = signal(false);

  // Public read-only signals
  readonly customers = this._customers.asReadonly();
  readonly loading = this._loading.asReadonly();
}
```

## Configuration with InjectionTokens

Use `InjectionToken` for extension points with factory defaults:

```typescript
export const CUSTOMER_API_URL = new InjectionToken<string>(
  '<ANGULAR_PREFIX>.apiUrl',
  { factory: () => '/api/v1/customers' }
);
```

Expose a `provideXxx()` function for consumers:

```typescript
export function provideCustomerRegistry(
  config?: Partial<CustomerRegistryConfig>
): EnvironmentProviders {
  return makeEnvironmentProviders([...]);
}
```

## i18n

Use locale tokens with a fallback chain:
`host overrides -> built-in[locale] -> built-in['en'] -> key`

Pipes should be `pure: true` with internal caching for performance.

## CSS Custom Properties

Use module-prefixed custom properties for theming:

```css
:host {
  --<ANGULAR_PREFIX>-primary: var(--<ANGULAR_PREFIX>-primary, #1976d2);
}
```

## Common Pitfalls

- NEVER name an `@Input()` as `formControl` -- it collides with Angular's
  `FormControlDirective` selector. Use `control` instead.
- `ts-node` is required as a devDependency for Jest to parse `.ts` config.
- ng-packagr requires at least one export in `public-api.ts`.
- Always unsubscribe from observables or use `takeUntilDestroyed()`.
