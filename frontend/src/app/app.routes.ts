import { Routes } from '@angular/router';
import { InventoryComponent } from './inventory.component';

export const routes: Routes = [
  { path: 'inventory', component: InventoryComponent },
  { path: '', redirectTo: '/inventory', pathMatch: 'full' }
];
