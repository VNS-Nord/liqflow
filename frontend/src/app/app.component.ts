import { Component } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div
      style="display: flex; height: 100vh; background-color: #f3f4f6; overflow: hidden; font-family: sans-serif;"
    >
      <!-- Tamsus šoninis meniu -->
      <aside
        style="width: 260px; background-color: #0f172a; color: white; display: flex; flex-direction: column; flex-shrink: 0; box-shadow: 4px 0 10px rgba(0,0,0,0.1);"
      >
        <div
          style="height: 64px; display: flex; align-items: center; padding: 0 24px; font-size: 20px; font-weight: bold; background-color: #020617; border-bottom: 1px solid #1e293b;"
        >
          📦 LiqFlow
        </div>
        <nav style="padding: 24px 16px; display: flex; flex-direction: column; gap: 8px;">
          <a
            routerLink="/inventory"
            routerLinkActive="active-link"
            style="display: flex; align-items: center; gap: 12px; padding: 10px 16px; border-radius: 8px; color: #cbd5e1; text-decoration: none; transition: 0.2s;"
          >
            📊 Prekių likučiai
          </a>
          <a
            href="#"
            style="display: flex; align-items: center; gap: 12px; padding: 10px 16px; border-radius: 8px; color: #cbd5e1; text-decoration: none; transition: 0.2s;"
          >
            🔄 Perkėlimai
          </a>
          <a
            href="#"
            style="display: flex; align-items: center; gap: 12px; padding: 10px 16px; border-radius: 8px; color: #cbd5e1; text-decoration: none; transition: 0.2s;"
          >
            🏢 Lokacijos
          </a>
        </nav>
      </aside>

      <!-- Pagrindinis turinys -->
      <div style="flex: 1; display: flex; flex-direction: column; min-width: 0; overflow: hidden;">
        <header
          style="height: 64px; background-color: white; border-bottom: 1px solid #e5e7eb; display: flex; align-items: center; justify-content: space-between; padding: 0 32px; box-shadow: 0 1px 2px rgba(0,0,0,0.05); flex-shrink: 0;"
        >
          <span style="color: #374151; font-weight: 600;">Sandėlio valdymo sistema</span>
          <span style="font-size: 14px; font-weight: 500; color: #4b5563;">Admin</span>
        </header>

        <main style="flex: 1; padding: 32px; overflow-y: auto;">
          <div
            style="background-color: white; border-radius: 12px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); border: 1px solid #e5e7eb; padding: 24px;"
          >
            <router-outlet></router-outlet>
          </div>
        </main>
      </div>
    </div>
  `,
  styles: `
    .active-link {
      background-color: #2563eb !important;
      color: white !important;
      font-weight: 500;
    }
    a:hover {
      background-color: #1e293b;
      color: white;
    }
  `,
})
export class AppComponent {
  title = 'liqflow-ui';
}
