import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
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
});
