import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  input,
  Output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { TranslatePipe } from '../../i18n/translate.pipe';
import { Customer } from '../../models/customer.model';

@Component({
  selector: 'crui-customer-list',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatProgressBarModule,
    MatIconModule,
    MatButtonModule,
    TranslatePipe,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (loading()) {
      <mat-progress-bar mode="indeterminate"></mat-progress-bar>
    }

    <div class="crui-list-container">
      @if (!loading() && customers().length === 0) {
        <p class="crui-no-results">{{ 'label.noResults' | translate }}</p>
      } @else {
        <table mat-table [dataSource]="customers()" matSort (matSortChange)="onSort($event)" class="crui-table">

          <ng-container matColumnDef="type">
            <th mat-header-cell *matHeaderCellDef mat-sort-header>{{ 'field.type' | translate }}</th>
            <td mat-cell *matCellDef="let customer">{{ 'customer.type.' + customer.type | translate }}</td>
          </ng-container>

          <ng-container matColumnDef="document">
            <th mat-header-cell *matHeaderCellDef mat-sort-header>{{ 'field.document' | translate }}</th>
            <td mat-cell *matCellDef="let customer">{{ customer.document }}</td>
          </ng-container>

          <ng-container matColumnDef="displayName">
            <th mat-header-cell *matHeaderCellDef mat-sort-header>{{ 'field.displayName' | translate }}</th>
            <td mat-cell *matCellDef="let customer">{{ customer.displayName }}</td>
          </ng-container>

          <ng-container matColumnDef="status">
            <th mat-header-cell *matHeaderCellDef mat-sort-header>{{ 'field.status' | translate }}</th>
            <td mat-cell *matCellDef="let customer">
              <span class="crui-status-badge crui-status-{{ customer.status | lowercase }}">
                {{ 'customer.status.' + customer.status | translate }}
              </span>
            </td>
          </ng-container>

          <ng-container matColumnDef="actions">
            <th mat-header-cell *matHeaderCellDef>{{ 'label.actions' | translate }}</th>
            <td mat-cell *matCellDef="let customer">
              <button mat-icon-button (click)="onSelect(customer); $event.stopPropagation()" aria-label="View details">
                <mat-icon>visibility</mat-icon>
              </button>
            </td>
          </ng-container>

          <tr mat-header-row *matHeaderRowDef="displayedColumns()"></tr>
          <tr mat-row *matRowDef="let row; columns: displayedColumns();"
              (click)="onSelect(row)"
              class="crui-clickable-row"></tr>
        </table>

        <mat-paginator
          [length]="totalCount()"
          [pageSize]="pageSize()"
          [pageIndex]="pageIndex()"
          [pageSizeOptions]="[10, 20, 50]"
          (page)="onPage($event)"
          showFirstLastButtons>
        </mat-paginator>
      }
    </div>
  `,
  styles: [`
    .crui-list-container {
      width: 100%;
    }
    .crui-table {
      width: 100%;
    }
    .crui-clickable-row {
      cursor: pointer;
    }
    .crui-clickable-row:hover {
      background-color: var(--crui-hover-bg, rgba(0, 0, 0, 0.04));
    }
    .crui-no-results {
      text-align: center;
      padding: var(--crui-spacing-lg, 24px);
      color: var(--crui-text-secondary, rgba(0, 0, 0, 0.54));
    }
    .crui-status-badge {
      padding: 2px 8px;
      border-radius: 12px;
      font-size: 12px;
      font-weight: 500;
    }
    .crui-status-draft { background: var(--crui-status-draft-bg, #e0e0e0); }
    .crui-status-active { background: var(--crui-status-active-bg, #c8e6c9); color: #2e7d32; }
    .crui-status-suspended { background: var(--crui-status-suspended-bg, #fff9c4); color: #f57f17; }
    .crui-status-closed { background: var(--crui-status-closed-bg, #ffcdd2); color: #c62828; }
  `],
})
export class CustomerListComponent {
  /** Array of customers to display in the table. Default: empty array. */
  readonly customers = input<Customer[]>([]);

  /** Total number of customers (used by the paginator). Default: 0. */
  readonly totalCount = input(0);

  /** Number of customers per page. Default: 20. */
  readonly pageSize = input(20);

  /** Current page index (0-based). Default: 0. */
  readonly pageIndex = input(0);

  /** Whether a loading indicator should be shown. Default: false. */
  readonly loading = input(false);

  /** Column keys to display. Default: ['type', 'document', 'displayName', 'status', 'actions']. */
  readonly displayedColumns = input<string[]>(['type', 'document', 'displayName', 'status', 'actions']);

  /** Emits a PageEvent when the user navigates to a different page */
  @Output() readonly pageChange = new EventEmitter<PageEvent>();

  /** Emits a Sort event when the user changes the column sort */
  @Output() readonly sortChange = new EventEmitter<Sort>();

  /** Emits the selected customer when the user clicks a row */
  @Output() readonly selectCustomer = new EventEmitter<Customer>();

  onPage(event: PageEvent): void {
    this.pageChange.emit(event);
  }

  onSort(event: Sort): void {
    this.sortChange.emit(event);
  }

  onSelect(customer: Customer): void {
    this.selectCustomer.emit(customer);
  }
}
