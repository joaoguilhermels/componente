import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { FieldRendererContext } from 'customer-registry-ui';

/**
 * Custom renderer for the loyalty number field.
 * Demonstrates how host apps create custom field renderers
 * that integrate with the library's SafeFieldRendererHostComponent.
 */
@Component({
  selector: 'app-loyalty-renderer',
  standalone: true,
  imports: [CommonModule, MatFormFieldModule, MatInputModule, MatIconModule],
  template: `
    <mat-form-field appearance="outline" class="loyalty-field">
      <mat-label>Loyalty Number</mat-label>
      <input matInput
             [value]="context?.value ?? ''"
             (input)="onInput($event)"
             [disabled]="context?.disabled ?? false"
             placeholder="LOYALTY-XXXX" />
      <mat-icon matSuffix>star</mat-icon>
    </mat-form-field>
  `,
  styles: [`
    .loyalty-field { width: 100%; }
    mat-icon { color: #ffd700; }
  `],
})
export class LoyaltyRendererComponent {
  @Input() context?: FieldRendererContext;

  onInput(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.context?.onChange(value);
  }
}
