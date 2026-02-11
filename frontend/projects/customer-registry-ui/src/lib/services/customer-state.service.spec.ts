import { TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { CustomerStateService } from './customer-state.service';
import { CustomerRegistryApiClient } from './customer-registry-api-client.service';
import {
  Customer,
  CustomerPageResponse,
} from '../models/customer.model';

describe('CustomerStateService', () => {
  let service: CustomerStateService;
  let apiMock: jest.Mocked<CustomerRegistryApiClient>;

  const mockCustomer: Customer = {
    id: '550e8400-e29b-41d4-a716-446655440000',
    type: 'PF',
    document: '52998224725',
    displayName: 'Maria Silva',
    status: 'ACTIVE',
    addresses: [],
    contacts: [],
    schemaVersion: 1,
    attributes: {},
    createdAt: '2025-01-01T00:00:00Z',
    updatedAt: '2025-01-01T00:00:00Z',
  };

  const mockPageResponse: CustomerPageResponse = {
    content: [mockCustomer],
    totalElements: 1,
    totalPages: 1,
    page: 0,
    size: 20,
  };

  beforeEach(() => {
    apiMock = {
      create: jest.fn(),
      findById: jest.fn(),
      findByDocument: jest.fn(),
      search: jest.fn(),
      update: jest.fn(),
      delete: jest.fn(),
    } as unknown as jest.Mocked<CustomerRegistryApiClient>;

    TestBed.configureTestingModule({
      providers: [
        CustomerStateService,
        { provide: CustomerRegistryApiClient, useValue: apiMock },
      ],
    });
    service = TestBed.inject(CustomerStateService);
  });

  afterEach(() => TestBed.resetTestingModule());

  describe('initial state', () => {
    it('should start with empty customers', () => {
      expect(service.customers()).toEqual([]);
    });

    it('should start with no selected customer', () => {
      expect(service.selectedCustomer()).toBeNull();
    });

    it('should start not loading', () => {
      expect(service.loading()).toBe(false);
    });

    it('should start with no error', () => {
      expect(service.error()).toBeNull();
    });

    it('should compute isEmpty as true', () => {
      expect(service.isEmpty()).toBe(true);
    });
  });

  describe('loadCustomers', () => {
    it('should set loading while fetching', () => {
      apiMock.search.mockReturnValue(of(mockPageResponse));
      service.loadCustomers();

      // After subscribe completes synchronously, loading should be false
      expect(service.loading()).toBe(false);
      expect(service.customers()).toEqual([mockCustomer]);
      expect(service.totalCount()).toBe(1);
    });

    it('should set error on failure', () => {
      apiMock.search.mockReturnValue(
        throwError(() => ({ error: { detail: 'Server error' } }))
      );
      service.loadCustomers();

      expect(service.error()).toBe('Server error');
      expect(service.loading()).toBe(false);
    });

    it('should merge search params with pagination defaults', () => {
      apiMock.search.mockReturnValue(of(mockPageResponse));
      service.loadCustomers({ type: 'PF' });

      expect(apiMock.search).toHaveBeenCalledWith(
        expect.objectContaining({
          type: 'PF',
          page: 0,
          size: 20,
        })
      );
    });

    it('should clear error before loading', () => {
      // Set an error first
      apiMock.search.mockReturnValue(
        throwError(() => ({ error: { detail: 'Error' } }))
      );
      service.loadCustomers();
      expect(service.error()).toBe('Error');

      // Now load successfully
      apiMock.search.mockReturnValue(of(mockPageResponse));
      service.loadCustomers();
      expect(service.error()).toBeNull();
    });
  });

  describe('loadCustomer', () => {
    it('should set selectedCustomer on success', () => {
      apiMock.findById.mockReturnValue(of(mockCustomer));
      service.loadCustomer(mockCustomer.id);

      expect(service.selectedCustomer()).toEqual(mockCustomer);
      expect(service.loading()).toBe(false);
    });

    it('should set error on failure', () => {
      apiMock.findById.mockReturnValue(
        throwError(() => ({ message: 'Not found' }))
      );
      service.loadCustomer('unknown-id');

      expect(service.error()).toBe('Not found');
      expect(service.loading()).toBe(false);
    });
  });

  describe('state management', () => {
    it('should clear error', () => {
      apiMock.search.mockReturnValue(
        throwError(() => ({ error: { detail: 'Error' } }))
      );
      service.loadCustomers();
      expect(service.error()).toBe('Error');

      service.clearError();
      expect(service.error()).toBeNull();
    });

    it('should clear selection', () => {
      apiMock.findById.mockReturnValue(of(mockCustomer));
      service.loadCustomer(mockCustomer.id);
      expect(service.selectedCustomer()).toEqual(mockCustomer);

      service.clearSelection();
      expect(service.selectedCustomer()).toBeNull();
    });

    it('should update page and reload', () => {
      apiMock.search.mockReturnValue(
        of({ ...mockPageResponse, page: 2 })
      );
      service.goToPage(2);

      expect(service.currentPage()).toBe(2);
      expect(apiMock.search).toHaveBeenCalled();
    });
  });

  describe('subscription management', () => {
    it('should cancel previous search when loading new customers', () => {
      const subject1 = new Subject<CustomerPageResponse>();
      const subject2 = new Subject<CustomerPageResponse>();

      apiMock.search
        .mockReturnValueOnce(subject1.asObservable())
        .mockReturnValueOnce(subject2.asObservable());

      service.loadCustomers();
      // First request is in flight
      expect(service.loading()).toBe(true);

      service.loadCustomers();
      // Second request replaces first

      // Complete second request
      subject2.next(mockPageResponse);
      subject2.complete();

      expect(service.customers()).toEqual([mockCustomer]);
      expect(service.loading()).toBe(false);

      // First request completing late should have no effect (unsubscribed)
      subject1.next({ ...mockPageResponse, content: [] });
      subject1.complete();

      // Customers should still be from second request
      expect(service.customers()).toEqual([mockCustomer]);
    });

    it('should cancel previous detail load when loading new customer', () => {
      const subject1 = new Subject<Customer>();
      const subject2 = new Subject<Customer>();

      apiMock.findById
        .mockReturnValueOnce(subject1.asObservable())
        .mockReturnValueOnce(subject2.asObservable());

      service.loadCustomer('id-1');
      service.loadCustomer('id-2');

      // Complete second request
      subject2.next(mockCustomer);
      subject2.complete();

      expect(service.selectedCustomer()).toEqual(mockCustomer);

      // First request completing late should have no effect
      subject1.next({ ...mockCustomer, displayName: 'Stale' });
      subject1.complete();

      expect(service.selectedCustomer()?.displayName).toBe('Maria Silva');
    });
  });

  describe('ngOnDestroy', () => {
    it('should unsubscribe search subscription on destroy', () => {
      const subject = new Subject<CustomerPageResponse>();
      apiMock.search.mockReturnValue(subject.asObservable());

      service.loadCustomers();
      expect(service.loading()).toBe(true);

      service.ngOnDestroy();

      // Completing after destroy should have no effect
      subject.next(mockPageResponse);
      subject.complete();

      // Still loading because the subscriber was removed before next() was received
      expect(service.loading()).toBe(true);
    });

    it('should unsubscribe detail subscription on destroy', () => {
      const subject = new Subject<Customer>();
      apiMock.findById.mockReturnValue(subject.asObservable());

      service.loadCustomer('id-1');
      expect(service.loading()).toBe(true);

      service.ngOnDestroy();

      subject.next(mockCustomer);
      subject.complete();

      expect(service.selectedCustomer()).toBeNull();
    });

    it('should not throw when called without active subscriptions', () => {
      expect(() => service.ngOnDestroy()).not.toThrow();
    });
  });

  describe('unexpected error shapes', () => {
    it('should handle error with empty error object', () => {
      apiMock.search.mockReturnValue(
        throwError(() => ({ error: {} }))
      );
      service.loadCustomers();

      expect(service.error()).toBe('Unknown error');
      expect(service.loading()).toBe(false);
    });

    it('should handle plain string error', () => {
      apiMock.search.mockReturnValue(
        throwError(() => 'Something went wrong')
      );
      service.loadCustomers();

      expect(service.error()).toBe('Unknown error');
      expect(service.loading()).toBe(false);
    });

    it('should handle null error', () => {
      apiMock.search.mockReturnValue(
        throwError(() => null)
      );
      service.loadCustomers();

      expect(service.error()).toBe('Unknown error');
      expect(service.loading()).toBe(false);
    });

    it('should handle error with message but no error.detail in loadCustomer', () => {
      apiMock.findById.mockReturnValue(
        throwError(() => ({ error: {} }))
      );
      service.loadCustomer('some-id');

      expect(service.error()).toBe('Unknown error');
      expect(service.loading()).toBe(false);
    });
  });
});
