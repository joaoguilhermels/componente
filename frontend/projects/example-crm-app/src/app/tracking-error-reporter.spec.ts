import { TrackingErrorReporter } from './tracking-error-reporter';

describe('TrackingErrorReporter', () => {
  let reporter: TrackingErrorReporter;

  beforeEach(() => {
    reporter = new TrackingErrorReporter();
  });

  it('should start with no errors', () => {
    expect(reporter.errorCount).toBe(0);
    expect(reporter.errors).toHaveLength(0);
  });

  it('should track reported errors', () => {
    reporter.report('test-renderer', new Error('test error'));
    expect(reporter.errorCount).toBe(1);
    expect(reporter.hasErrorFor('test-renderer')).toBe(true);
  });

  it('should not find unreported renderer IDs', () => {
    reporter.report('renderer-a', new Error('error a'));
    expect(reporter.hasErrorFor('renderer-b')).toBe(false);
  });

  it('should clear all errors', () => {
    reporter.report('r1', 'error 1');
    reporter.report('r2', 'error 2');
    expect(reporter.errorCount).toBe(2);

    reporter.clear();
    expect(reporter.errorCount).toBe(0);
    expect(reporter.errors).toHaveLength(0);
  });
});
