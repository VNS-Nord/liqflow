import { Component, inject, signal, OnInit } from '@angular/core';

import { Inventory } from './inventory';
import { InventoryService, LocationOption } from './inventory.service';

@Component({
  selector: 'app-inventory',
  standalone: true,
  template: `
    <!-- Valdymo blokas: Sandėlio pasirinkimas + Puslapio dydis + Atnaujinimo mygtukas -->
    <div
      style="margin-bottom: 20px; display: flex; gap: 15px; align-items: center; flex-wrap: wrap;"
    >
      <div>
        <label for="locationSelect" style="font-weight: bold; margin-right: 8px;">Sandėlis:</label>
        <select
          id="locationSelect"
          (change)="onLocationChange($event)"
          style="padding: 8px; border-radius: 4px; border: 1px solid #ccc;"
        >
          <option value="">Visi sandėliai (Bendra lentelė)</option>
          @for (loc of locations(); track loc.id) {
            <option [value]="loc.id">{{ loc.code }} — {{ loc.name }}</option>
          }
        </select>
      </div>

      <div>
        <label for="pageSizeSelect" style="font-weight: bold; margin-right: 8px;">Rodyti po:</label>
        <select
          id="pageSizeSelect"
          (change)="onPageSizeChange($event)"
          style="padding: 8px; border-radius: 4px; border: 1px solid #ccc;"
        >
          <option [value]="5" [selected]="pageSize() === 5">5</option>
          <option [value]="10" [selected]="pageSize() === 10">10</option>
          <option [value]="20" [selected]="pageSize() === 20">20</option>
          <option [value]="50" [selected]="pageSize() === 50">50</option>
          <option [value]="100" [selected]="pageSize() === 100">100</option>
        </select>
      </div>

      <button
        (click)="loadInventories()"
        [disabled]="loading()"
        style="padding: 8px 16px; cursor: pointer; border-radius: 4px; background: #2563eb; color: white; border: none;"
      >
        {{ loading() ? 'Kraunama...' : 'Atnaujinti' }}
      </button>
    </div>

    @if (error()) {
      <p role="alert" style="color: #b42318; margin-bottom: 15px;">{{ error() }}</p>
    }

    @if (loaded()) {
      <table
        style="width: 100%; border-collapse: collapse; background: white; border: 1px solid #e5e7eb;"
      >
        <thead>
          <tr style="background-color: #f9fafb; text-align: left;">
            <th style="padding: 12px 16px; border-bottom: 1px solid #e5e7eb;">Lokacija</th>
            <th style="padding: 12px 16px; border-bottom: 1px solid #e5e7eb;">SKU</th>
            <th style="padding: 12px 16px; border-bottom: 1px solid #e5e7eb;">Prekė</th>
            <th style="padding: 12px 16px; border-bottom: 1px solid #e5e7eb;">Fizinis likutis</th>
            <th style="padding: 12px 16px; border-bottom: 1px solid #e5e7eb;">Rezervuota</th>
            <th style="padding: 12px 16px; border-bottom: 1px solid #e5e7eb;">Prieinama</th>
          </tr>
        </thead>

        <tbody>
          @for (item of inventories(); track item.id) {
            <tr style="border-bottom: 1px solid #e5e7eb;">
              <td style="padding: 12px 16px;">{{ item.locationCode }}</td>
              <td style="padding: 12px 16px;">{{ item.productSku }}</td>
              <td style="padding: 12px 16px;">{{ item.productName }}</td>
              <td style="padding: 12px 16px;">{{ item.quantity }}</td>
              <td style="padding: 12px 16px;">{{ item.reservedQuantity }}</td>

              <td
                style="padding: 12px 16px;"
                [class.low-stock]="item.availableQuantity <= item.minThreshold"
              >
                {{ item.availableQuantity }}
                @if (item.availableQuantity <= item.minThreshold) {
                  <span style="color: #b42318; font-weight: bold;"> — mažas likutis</span>
                }
              </td>
            </tr>
          } @empty {
            <tr>
              <td colspan="6" style="padding: 20px; text-align: center; color: #666;">
                Likučių įrašų nėra.
              </td>
            </tr>
          }
        </tbody>
      </table>

      <div style="margin-top: 20px; display: flex; gap: 15px; align-items: center;">
        <button
          (click)="previousPage()"
          [disabled]="currentPage() === 0 || loading()"
          style="padding: 8px 16px; cursor: pointer;"
        >
          Atgal
        </button>
        <span>Puslapis {{ currentPage() + 1 }} iš {{ totalPages() || 1 }}</span>
        <button
          (click)="nextPage()"
          [disabled]="currentPage() >= totalPages() - 1 || totalPages() === 0 || loading()"
          style="padding: 8px 16px; cursor: pointer;"
        >
          Pirmyn
        </button>
      </div>
    }
  `,
  styles: `
    :host {
      display: block;
    }
    .low-stock {
      color: #b42318;
      font-weight: bold;
    }
  `,
})
export class InventoryComponent implements OnInit {
  private readonly inventoryService = inject(InventoryService);

  readonly inventories = signal<Inventory[]>([]);
  readonly locations = signal<LocationOption[]>([]);

  readonly selectedLocation = signal<string>('');
  readonly currentPage = signal<number>(0);
  readonly pageSize = signal<number>(5);
  readonly totalPages = signal<number>(0);

  readonly loading = signal(false);
  readonly loaded = signal(false);
  readonly error = signal('');

  ngOnInit(): void {
    this.loadLocations();
    this.loadInventories();
  }

  loadLocations(): void {
    this.inventoryService.getLocations().subscribe({
      next: (response: any) => {
        const locs = response.content ? response.content : response;
        this.locations.set(locs);
      },
      error: () => console.warn('Nepavyko užkrauti lokacijų sąrašo'),
    });
  }

  loadInventories(): void {
    const locationId = this.selectedLocation();
    if (locationId) {
      this.loadByLocation(locationId);
    } else {
      this.loadAll();
    }
  }

  loadAll(): void {
    this.loading.set(true);
    this.loaded.set(false);
    this.error.set('');

    this.inventoryService.getAll(this.currentPage(), this.pageSize()).subscribe({
      next: (response) => {
        const items = response.content ? response.content : response;
        this.inventories.set(items);
        this.totalPages.set(response.totalPages ?? 1);
        this.loaded.set(true);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Nepavyko gauti likučių. Patikrink, ar veikia LiqFlow API.');
        this.loading.set(false);
      },
    });
  }

  private loadByLocation(locationId: string): void {
    this.loading.set(true);
    this.loaded.set(false);
    this.error.set('');

    this.inventoryService.getByLocation(locationId, this.currentPage(), this.pageSize()).subscribe({
      next: (response) => {
        const items = response.content ? response.content : response;
        this.inventories.set(items);
        this.totalPages.set(response.totalPages ?? 1);
        this.loaded.set(true);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Nepavyko gauti likučių pagal lokaciją. Patikrink, ar veikia LiqFlow API.');
        this.loading.set(false);
      },
    });
  }

  nextPage(): void {
    if (this.currentPage() < this.totalPages() - 1) {
      this.currentPage.update((p) => p + 1);
      this.loadInventories();
    }
  }

  previousPage(): void {
    if (this.currentPage() > 0) {
      this.currentPage.update((p) => p - 1);
      this.loadInventories();
    }
  }

  onLocationChange(event: Event): void {
    const locationId = (event.target as HTMLSelectElement).value;
    this.selectedLocation.set(locationId);
    this.currentPage.set(0);
    this.loadInventories();
  }

  onPageSizeChange(event: Event): void {
    const newSize = Number((event.target as HTMLSelectElement).value);
    this.pageSize.set(newSize);
    this.currentPage.set(0);
    this.loadInventories();
  }
}
