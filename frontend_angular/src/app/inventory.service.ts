import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Inventory } from './inventory';

export interface LocationOption {
  id: string;
  code: string;
  name: string;
}

@Injectable({ providedIn: 'root' })
export class InventoryService {
  private readonly http = inject(HttpClient);

  getAll(page: number = 0, size: number = 5): Observable<any> {
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http.get<any>('/api/v1/inventories', { params });
  }

  getByLocation(locationId: string, page: number = 0, size: number = 5): Observable<any> {
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http.get<any>(`/api/v1/inventories/location/${locationId}`, { params });
  }

  getLocations(): Observable<LocationOption[]> {
    return this.http.get<LocationOption[]>('/api/v1/locations');
  }
}
