import { TestBed } from '@angular/core/testing';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { CustomerRegistryApiClient } from './customer-registry-api-client.service';
import { CUSTOMER_REGISTRY_UI_CONFIG } from '../tokens';
import { DEFAULT_CONFIG } from '../models/config.model';
import {
  Customer,
  CustomerPageResponse,
} from '../models/customer.model';

describe('CustomerRegistryApiClient', () => {
  let client: CustomerRegistryApiClient;
  let httpMock: HttpTestingController;

  const baseUrl = '/api/v1/customers';

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

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        CustomerRegistryApiClient,
        { provide: CUSTOMER_REGISTRY_UI_CONFIG, useValue: DEFAULT_CONFIG },
      ],
    });
    client = TestBed.inject(CustomerRegistryApiClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    TestBed.resetTestingModule();
  });

  describe('create', () => {
    it('should POST to /customers', () => {
      const request = { type: 'PF' as const, document: '52998224725', displayName: 'Maria Silva' };

      client.create(request).subscribe((result) => {
        expect(result).toEqual(mockCustomer);
      });

      const req = httpMock.expectOne(baseUrl);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(request);
      req.flush(mockCustomer);
    });
  });

  describe('findById', () => {
    it('should GET /customers/:id', () => {
      const id = '550e8400-e29b-41d4-a716-446655440000';

      client.findById(id).subscribe((result) => {
        expect(result).toEqual(mockCustomer);
      });

      const req = httpMock.expectOne(`${baseUrl}/${id}`);
      expect(req.request.method).toBe('GET');
      req.flush(mockCustomer);
    });
  });

  describe('findByDocument', () => {
    it('should GET /customers/by-document/:document', () => {
      const document = '52998224725';

      client.findByDocument(document).subscribe((result) => {
        expect(result).toEqual(mockCustomer);
      });

      const req = httpMock.expectOne(`${baseUrl}/by-document/${document}`);
      expect(req.request.method).toBe('GET');
      req.flush(mockCustomer);
    });
  });

  describe('search', () => {
    it('should GET /customers with query params', () => {
      const pageResponse: CustomerPageResponse = {
        customers: [mockCustomer],
        totalElements: 1,
        totalPages: 1,
        page: 0,
        size: 20,
      };

      client
        .search({ type: 'PF', status: 'ACTIVE', page: 0, size: 20 })
        .subscribe((result) => {
          expect(result).toEqual(pageResponse);
        });

      const req = httpMock.expectOne((r) => r.url === baseUrl);
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('type')).toBe('PF');
      expect(req.request.params.get('status')).toBe('ACTIVE');
      expect(req.request.params.get('page')).toBe('0');
      expect(req.request.params.get('size')).toBe('20');
      req.flush(pageResponse);
    });

    it('should omit undefined params', () => {
      client.search({}).subscribe();

      const req = httpMock.expectOne(baseUrl);
      expect(req.request.params.keys().length).toBe(0);
      req.flush({ content: [], totalElements: 0, totalPages: 0, page: 0, size: 20 });
    });
  });

  describe('update', () => {
    it('should PATCH /customers/:id', () => {
      const id = '550e8400-e29b-41d4-a716-446655440000';
      const request = { displayName: 'Maria Santos' };

      client.update(id, request).subscribe((result) => {
        expect(result).toEqual({ ...mockCustomer, displayName: 'Maria Santos' });
      });

      const req = httpMock.expectOne(`${baseUrl}/${id}`);
      expect(req.request.method).toBe('PATCH');
      expect(req.request.body).toEqual(request);
      req.flush({ ...mockCustomer, displayName: 'Maria Santos' });
    });
  });

  describe('delete', () => {
    it('should DELETE /customers/:id', () => {
      const id = '550e8400-e29b-41d4-a716-446655440000';

      client.delete(id).subscribe();

      const req = httpMock.expectOne(`${baseUrl}/${id}`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('HTTP error handling', () => {
    it('should propagate 400 Bad Request errors', (done) => {
      client.create({ type: 'PF', document: 'invalid', displayName: 'Test' }).subscribe({
        error: (err) => {
          expect(err.status).toBe(400);
          done();
        },
      });

      const req = httpMock.expectOne(baseUrl);
      req.flush({ detail: 'Invalid document' }, { status: 400, statusText: 'Bad Request' });
    });

    it('should propagate 404 Not Found errors', (done) => {
      const id = '550e8400-e29b-41d4-a716-446655440000';
      client.findById(id).subscribe({
        error: (err) => {
          expect(err.status).toBe(404);
          done();
        },
      });

      const req = httpMock.expectOne(`${baseUrl}/${id}`);
      req.flush({ detail: 'Not found' }, { status: 404, statusText: 'Not Found' });
    });

    it('should propagate 409 Conflict errors', (done) => {
      client.create({ type: 'PF', document: '52998224725', displayName: 'Test' }).subscribe({
        error: (err) => {
          expect(err.status).toBe(409);
          done();
        },
      });

      const req = httpMock.expectOne(baseUrl);
      req.flush({ detail: 'Duplicate document' }, { status: 409, statusText: 'Conflict' });
    });

    it('should propagate 500 Internal Server Error', (done) => {
      client.findById('some-id').subscribe({
        error: (err) => {
          expect(err.status).toBe(500);
          done();
        },
      });

      const req = httpMock.expectOne(`${baseUrl}/some-id`);
      req.flush({ detail: 'Internal error' }, { status: 500, statusText: 'Internal Server Error' });
    });
  });
});

describe('CustomerRegistryApiClient with custom apiBaseUrl', () => {
  let client: CustomerRegistryApiClient;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        CustomerRegistryApiClient,
        {
          provide: CUSTOMER_REGISTRY_UI_CONFIG,
          useValue: { ...DEFAULT_CONFIG, apiBaseUrl: '/custom-api/v2' },
        },
      ],
    });
    client = TestBed.inject(CustomerRegistryApiClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    TestBed.resetTestingModule();
  });

  it('should use custom apiBaseUrl for requests', () => {
    const mockCustomer: Customer = {
      id: 'test-id',
      type: 'PF',
      document: '52998224725',
      displayName: 'Test',
      status: 'ACTIVE',
      addresses: [],
      contacts: [],
      schemaVersion: 1,
      attributes: {},
      createdAt: '2025-01-01T00:00:00Z',
      updatedAt: '2025-01-01T00:00:00Z',
    };

    client.findById('test-id').subscribe();

    const req = httpMock.expectOne('/custom-api/v2/customers/test-id');
    expect(req.request.method).toBe('GET');
    req.flush(mockCustomer);
  });
});
