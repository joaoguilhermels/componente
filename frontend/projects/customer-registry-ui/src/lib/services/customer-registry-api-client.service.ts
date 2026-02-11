import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  CreateCustomerRequest,
  Customer,
  CustomerPageResponse,
  CustomerSearchParams,
  UpdateCustomerRequest,
} from '../models/customer.model';
import { CUSTOMER_REGISTRY_UI_CONFIG } from '../tokens';

/**
 * HTTP client for the Customer Registry REST API.
 * Base URL is configured via CUSTOMER_REGISTRY_UI_CONFIG.
 *
 * **Timeout / Retry**: This client does not configure HTTP timeouts or retry logic.
 * Host applications should provide an `HttpInterceptor` to handle timeouts, retries,
 * and circuit-breaking as appropriate for their environment.
 */
@Injectable({ providedIn: 'root' })
export class CustomerRegistryApiClient {
  private readonly http = inject(HttpClient);
  private readonly config = inject(CUSTOMER_REGISTRY_UI_CONFIG);

  private get baseUrl(): string {
    return `${this.config.apiBaseUrl}/customers`;
  }

  create(request: CreateCustomerRequest): Observable<Customer> {
    return this.http.post<Customer>(this.baseUrl, request);
  }

  findById(id: string): Observable<Customer> {
    return this.http.get<Customer>(`${this.baseUrl}/${id}`);
  }

  findByDocument(document: string): Observable<Customer> {
    return this.http.get<Customer>(
      `${this.baseUrl}/by-document/${document}`
    );
  }

  search(params: CustomerSearchParams = {}): Observable<CustomerPageResponse> {
    let httpParams = new HttpParams();
    if (params.type) httpParams = httpParams.set('type', params.type);
    if (params.status) httpParams = httpParams.set('status', params.status);
    if (params.document) httpParams = httpParams.set('document', params.document);
    if (params.displayName) httpParams = httpParams.set('displayName', params.displayName);
    if (params.page != null) httpParams = httpParams.set('page', params.page.toString());
    if (params.size != null) httpParams = httpParams.set('size', params.size.toString());
    if (params.sort) httpParams = httpParams.set('sort', params.sort);

    return this.http.get<CustomerPageResponse>(this.baseUrl, {
      params: httpParams,
    });
  }

  update(id: string, request: UpdateCustomerRequest): Observable<Customer> {
    return this.http.patch<Customer>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
