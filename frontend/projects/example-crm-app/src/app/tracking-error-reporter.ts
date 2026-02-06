import { RendererErrorReporter } from 'customer-registry-ui';

/**
 * Custom error reporter that tracks renderer failures.
 * In a real app, this could send errors to a monitoring service.
 * Here it accumulates errors for testing and demonstration.
 */
export class TrackingErrorReporter implements RendererErrorReporter {
  readonly errors: Array<{ rendererId: string; error: unknown }> = [];

  report(rendererId: string, error: unknown): void {
    this.errors.push({ rendererId, error });
    console.warn(`[ExampleCRM] Renderer "${rendererId}" failed:`, error);
  }

  get errorCount(): number {
    return this.errors.length;
  }

  hasErrorFor(rendererId: string): boolean {
    return this.errors.some((e) => e.rendererId === rendererId);
  }

  clear(): void {
    this.errors.length = 0;
  }
}
