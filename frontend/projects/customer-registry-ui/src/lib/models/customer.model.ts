/**
 * Core domain models for the Customer Registry UI library.
 */

export type CustomerType = 'PF' | 'PJ';

export type CustomerStatus = 'DRAFT' | 'ACTIVE' | 'SUSPENDED' | 'CLOSED';

export interface Address {
  id: string;
  street: string;
  number: string;
  complement?: string;
  neighborhood: string;
  city: string;
  state: string;
  zipCode: string;
  country: string;
}

export interface Contact {
  id: string;
  type: 'EMAIL' | 'PHONE' | 'MOBILE';
  value: string;
  primary: boolean;
}

export interface Customer {
  id: string;
  type: CustomerType;
  document: string;
  displayName: string;
  status: CustomerStatus;
  addresses: Address[];
  contacts: Contact[];
  schemaVersion: number;
  attributes: Record<string, unknown>;
  createdAt: string;
  updatedAt: string;
}

export interface CreateCustomerRequest {
  type: CustomerType;
  document: string;
  displayName: string;
}

export interface UpdateCustomerRequest {
  displayName?: string;
  status?: CustomerStatus;
}

export interface CustomerSearchParams {
  type?: CustomerType;
  status?: CustomerStatus;
  document?: string;
  displayName?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export interface CustomerPageResponse {
  customers: Customer[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}
