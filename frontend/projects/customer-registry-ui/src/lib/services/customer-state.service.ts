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

  /** All customers in the current page */
  readonly customers = signal<Customer[]>([]);

  /** Currently selected customer (detail view) */
  readonly selectedCustomer = signal<Customer | null>(null);

  /** Whether an API operation is in progress */
  readonly loading = signal<boolean>(false);

  /** Last error message, or null if none */
  readonly error = signal<string | null>(null);

  /** Total number of customers matching the current search */
  readonly totalCount = signal<number>(0);

  /** Current page index (0-based) */
  readonly currentPage = signal<number>(0);

  /** Page size */
  readonly pageSize = signal<number>(20);

  /** Whether the customer list is empty */
  readonly isEmpty = computed(() => this.customers().length === 0 && !this.loading());

  /** Load customers with optional search params */
  loadCustomers(params: CustomerSearchParams = {}): void {
    this.loading.set(true);
    this.error.set(null);

    const searchParams: CustomerSearchParams = {
      page: this.currentPage(),
      size: this.pageSize(),
      ...params,
    };

    this.api.search(searchParams).subscribe({
      next: (response) => {
        this.customers.set(response.content);
        this.totalCount.set(response.totalElements);
        this.currentPage.set(response.page);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err.error?.detail ?? err.message ?? 'Unknown error');
        this.loading.set(false);
      },
    });
  }

  /** Load a single customer by ID */
  loadCustomer(id: string): void {
    this.loading.set(true);
    this.error.set(null);

    this.api.findById(id).subscribe({
      next: (customer) => {
        this.selectedCustomer.set(customer);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err.error?.detail ?? err.message ?? 'Unknown error');
        this.loading.set(false);
      },
    });
  }

  /** Clear current error */
  clearError(): void {
    this.error.set(null);
  }

  /** Clear selected customer */
  clearSelection(): void {
    this.selectedCustomer.set(null);
  }

  /** Update page and reload */
  goToPage(page: number): void {
    this.currentPage.set(page);
    this.loadCustomers();
  }
}
