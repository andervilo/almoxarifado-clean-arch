import { ChangeDetectionStrategy, Component, computed, inject, OnInit } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { PerfilUsuario } from '../../core/models/auth.model';

interface NavItem {
  label: string;
  route: string;
  roles: readonly PerfilUsuario[];
}

const NAV_ITEMS: readonly NavItem[] = [
  {
    label: 'Dashboard & Alertas',
    route: '/dashboard',
    roles: ['ADMIN_PLATAFORMA', 'ADMIN_ORGANIZACAO', 'ALMOXARIFE', 'GESTOR'],
  },
  {
    label: 'Catálogo',
    route: '/catalogo',
    roles: ['ADMIN_PLATAFORMA', 'ADMIN_ORGANIZACAO', 'ALMOXARIFE', 'GESTOR'],
  },
  {
    label: 'Estoque & Kardex',
    route: '/estoque',
    roles: ['ADMIN_PLATAFORMA', 'ADMIN_ORGANIZACAO', 'ALMOXARIFE', 'GESTOR'],
  },
  {
    label: 'Requisições',
    route: '/requisicoes',
    roles: ['ADMIN_PLATAFORMA', 'ADMIN_ORGANIZACAO', 'ALMOXARIFE', 'REQUISITANTE', 'GESTOR'],
  },
  {
    label: 'Inventário',
    route: '/inventario',
    roles: ['ADMIN_PLATAFORMA', 'ADMIN_ORGANIZACAO', 'ALMOXARIFE', 'GESTOR'],
  },
  {
    label: 'Relatórios',
    route: '/relatorios',
    roles: ['ADMIN_PLATAFORMA', 'ADMIN_ORGANIZACAO', 'ALMOXARIFE', 'GESTOR'],
  },
  {
    label: 'Configurações',
    route: '/configuracoes',
    roles: ['ADMIN_PLATAFORMA', 'ADMIN_ORGANIZACAO', 'ALMOXARIFE', 'GESTOR'],
  },
];

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="layout">
      <aside class="sidebar">
        <div class="brand">
          <span class="brand-title">Almoxarifado</span>
          <span class="brand-sub">MVP Operacional</span>
        </div>

        <nav class="nav" aria-label="Menu principal">
          @for (item of visibleNav(); track item.route) {
            <a
              class="nav-link"
              [routerLink]="item.route"
              routerLinkActive="active"
            >
              {{ item.label }}
            </a>
          }
        </nav>

        <div class="sidebar-footer">
          <div class="user-name">{{ auth.nomeUsuario() }}</div>
          <div class="user-roles">{{ auth.roles().join(', ') || 'Sem perfil' }}</div>
          @if (auth.organizacaoId(); as orgId) {
            <div class="user-org mono" title="Organização atual">Org: {{ orgId }}</div>
          }
          <button type="button" class="btn btn-secondary btn-sm logout-btn" (click)="auth.logout()">
            Sair
          </button>
        </div>
      </aside>

      <main class="main-content">
        <router-outlet />
      </main>
    </div>
  `,
  styles: `
    .layout {
      display: grid;
      grid-template-columns: 245px 1fr;
      min-height: 100vh;
    }
    .sidebar {
      background: var(--sidebar-bg);
      color: var(--sidebar-text);
      display: flex;
      flex-direction: column;
      padding: 1.25rem 0.9rem;
      gap: 1.25rem;
    }
    .brand {
      padding: 0 0.5rem;
      display: flex;
      flex-direction: column;
    }
    .brand-title {
      font-size: 1.15rem;
      font-weight: 700;
      color: #fff;
    }
    .brand-sub {
      font-size: 0.72rem;
      color: #94a3b8;
      text-transform: uppercase;
      letter-spacing: 0.06em;
    }
    .nav {
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
      flex: 1;
    }
    .nav-link {
      padding: 0.55rem 0.75rem;
      border-radius: 6px;
      color: var(--sidebar-text);
      font-size: 0.875rem;
      font-weight: 500;
      transition: background-color 0.15s ease, color 0.15s ease;

      &:hover {
        background: var(--sidebar-active);
        color: #fff;
      }
      &.active {
        background: var(--primary);
        color: #fff;
      }
    }
    .sidebar-footer {
      border-top: 1px solid #1e293b;
      padding-top: 0.9rem;
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
    }
    .user-name {
      font-weight: 600;
      color: #fff;
      font-size: 0.875rem;
    }
    .user-roles {
      font-size: 0.72rem;
      color: #94a3b8;
      word-break: break-word;
    }
    .user-org {
      font-size: 0.7rem;
      color: #64748b;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    .logout-btn {
      margin-top: 0.55rem;
      width: 100%;
    }
    .main-content {
      padding: 1.5rem 2rem;
      max-width: 1400px;
      width: 100%;
    }
    @media (max-width: 900px) {
      .layout {
        grid-template-columns: 1fr;
      }
      .main-content {
        padding: 1rem;
      }
    }
  `,
})
export class ShellComponent implements OnInit {
  protected readonly auth = inject(AuthService);

  protected readonly visibleNav = computed(() =>
    NAV_ITEMS.filter((item) => this.auth.hasAnyRole(item.roles)),
  );

  ngOnInit(): void {
    if (this.auth.isAuthenticated() && !this.auth.session()) {
      void this.auth.loadSessionMe();
    }
  }
}
