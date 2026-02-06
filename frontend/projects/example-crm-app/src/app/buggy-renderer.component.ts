import { Component } from '@angular/core';

/**
 * Intentionally buggy renderer that throws during construction.
 * Demonstrates the library's graceful degradation: when a custom renderer
 * fails, SafeFieldRendererHostComponent falls back to a standard Material
 * text input, keeping the form functional.
 */
@Component({
  selector: 'app-buggy-renderer',
  standalone: true,
  template: `<div>This should never render</div>`,
})
export class BuggyRendererComponent {
  constructor() {
    throw new Error('BuggyRendererComponent intentionally fails!');
  }
}
