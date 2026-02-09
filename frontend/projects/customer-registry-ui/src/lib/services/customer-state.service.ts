import { computed, inject, Injectable, signal } from '@angular/core';
import { Customer, CustomerSearchParams } from '../models/customer.model';
import { CustomerRegistryApiClient } from './customer-registry-api-client.service';

/**
 * Signal-based state management for the Customer Registry UI.
 *
 * Provides reactive state for the customer list and selected customer,
 * with loading/error tracking. Components can read signals directly
 * for fine-grained change detection (OnPush).
 */
@Injectable({ providedIn: 'root' })
export class CustomerStateService {
  private readonly api = inject(CustomerRegistryApiClient);

  /** All customers in the current page (internal writable signal) */
  private readonly _customers = signal<Customer[]>([]);

  /** Currently selected customer (internal writable signal) */
  private readonly _selectedCustomer = signal<Customer | null>(null);

  /** Whether an API operation is in progress (internal writable signal) */
  private readonly _loading = signal<boolean>(false);

  /** Last error message (internal writable signal) */
  private readonly _error = signal<string | null>(null);

  /** Total number of customers matching the current search (internal writable signal) */
  private readonly _totalCount = signal<number>(0);

  /** Current page index, 0-based (internal writable signal) */
  private readonly _currentPage = signal<number>(0);

  /** Page size (internal writable signal) */
  private readonly _pageSize = signal<number>(20);

  /** All customers in the current page */
  readonly customers = this._customers.asReadonly();

  /** Currently selected customer (detail view) */
  readonly selectedCustomer = this._selectedCustomer.asReadonly();

  /** Whether an API operation is in progress */
  readonly loading = this._loading.asReadonly();

  /** Last error message, or null if none */
  readonly error = this._error.asReadonly();

  /** Total number of customers matching the current search */
  readonly totalCount = this._totalCount.asReadonly();

  /** Current page index (0-based) */
  readonly currentPage = this._currentPage.asReadonly();

  /** Page size */
  readonly pageSize = this._pageSize.asReadonly();

  /** Whether the customer list is empty */
  readonly isEmpty = computed(() => this._customers().length === 0 && !this._loading());

  /** Load customers with optional search params */
  loadCustomers(params: CustomerSearchParams = {}): void {
    this._loading.set(true);
    this._error.set(null);

    const searchParams: CustomerSearchParams = {
      page: this._currentPage(),
      size: this._pageSize(),
      ...params,
    };

    this.api.search(searchParams).subscribe({
      next: (response) => {
        this._customers.set(response.content);
        this._totalCount.set(response.totalElements);
        this._currentPage.set(response.page);
        this._loading.set(false);
      },
      error: (err) => {
        this._error.set(this.extractErrorMessage(err));
        this._loading.set(false);
      },
    });
  }

  /** Load a single customer by ID */
  loadCustomer(id: string): void {
    this._loading.set(true);
    this._error.set(null);

    this.api.findById(id).subscribe({
      next: (customer) => {
        this._selectedCustomer.set(customer);
        this._loading.set(false);
      },
      error: (err) => {
        this._error.set(this.extractErrorMessage(err));
        this._loading.set(false);
      },
    });
  }

  /** Clear current error */
  clearError(): void {
    this._error.set(null);
  }

  /** Clear selected customer */
  clearSelection(): void {
    this._selectedCustomer.set(null);
  }

  /** Update page and reload */
  goToPage(page: number): void {
    this._currentPage.set(page);
    this.loadCustomers();
  }

  private extractErrorMessage(err: unknown): string {
    if (err == null || typeof err !== 'object') {
      return 'Unknown error';
    }
    const httpErr = err as Record<string, unknown>;
    const detail = (httpErr['error'] as Record<string, unknown> | undefined)?.['detail'];
    if (typeof detail === 'string') return detail;
    const message = httpErr['message'];
    if (typeof message === 'string') return message;
    return 'Unknown error';
  }
}
