# OneFinancial Customer Registry - Angular Frontend

Angular 17 workspace containing the `customer-registry-ui` library -- a reusable, configurable UI component library for customer registration and management.

## Workspace Structure

```
frontend/
  projects/
    customer-registry-ui/     # The library (ng-packagr)
      src/
        lib/
          components/          # Standalone UI components
          i18n/                # Internationalization (pt-BR, en)
          models/              # Domain and config models
          services/            # API client & state management
          tokens/              # Angular injection tokens
          validators/          # CPF/CNPJ validators
        public-api.ts          # Public API barrel
  dist/                        # Build output
```

## Prerequisites

All commands run inside Docker containers. **Do not run Node/npm locally.**

## Build

```bash
docker compose -f docker/docker-compose.yml run --rm node-build ng build customer-registry-ui --configuration production
```

## Test

```bash
docker compose -f docker/docker-compose.yml run --rm node-build ng test customer-registry-ui
```

## Library Features

- Signal-based state management (`CustomerStateService`)
- Configurable via `provideCustomerRegistry()` with feature flags
- i18n support (pt-BR, en) with runtime locale switching
- Extensible form with custom field renderers and validation rules
- Graceful degradation for custom renderers (`SafeFieldRendererHostComponent`)
- CPF/CNPJ document validation
- Angular Material UI components

## Configuration

```typescript
import { provideCustomerRegistry } from 'customer-registry-ui';

export const appConfig: ApplicationConfig = {
  providers: [
    provideCustomerRegistry({
      config: {
        apiBaseUrl: '/api/v1',
        locale: 'pt-BR',
        features: {
          search: true,
          addresses: true,
          contacts: true,
        },
      },
    }),
  ],
};
```

See the main project [README](../README.md) for full documentation.
