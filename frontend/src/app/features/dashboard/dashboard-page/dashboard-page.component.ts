import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { forkJoin } from 'rxjs';
import { extractApiErrorMessage } from '../../../core/services/api-error.util';
import { AlertBannerComponent } from '../../../shared/components/alert-banner/alert-banner.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { AlertaItem, DashboardService } from '../dashboard.service';

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [PageHeaderComponent, AlertBannerComponent, EmptyStateComponent, StatusBadgeComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-page-header
      title="Dashboard & Alertas"
      subtitle="Indicadores operacionais de estoque mínimo, validade próxima e lotes vencidos."
    >
      <button type="button" class="btn btn-secondary" [disabled]="loading()" (click)="carregar()">
        Atualizar
      </button>
    </app-page-header>

    <div class="stack">
      <app-alert-banner kind="error" [message]="erro()" />
      <app-alert-banner kind="success" [message]="sucesso()" />

      <div class="kpi-grid">
        @for (kpi of kpis(); track kpi.chave) {
          <div class="kpi-card">
            <div class="kpi-label">{{ kpi.label }}</div>
            <div class="kpi-value">{{ kpi.valor }}</div>
          </div>
        }
      </div>

      <section class="card">
        <h2 class="card-title">Alertas ativos ({{ alertas().length }})</h2>

        @if (alertas().length === 0) {
          <app-empty-state message="Nenhum alerta ativo no momento." />
        } @else {
          <div class="table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  <th>Tipo</th>
                  <th>Item</th>
                  <th>Almoxarifado</th>
                  <th>Quantidade</th>
                  <th>Limite / Validade</th>
                  <th>Ação</th>
                </tr>
              </thead>
              <tbody>
                @for (alerta of alertas(); track alerta.chave + ':' + alerta.revisao) {
                  <tr>
                    <td><app-status-badge [status]="alerta.tipo" /></td>
                    <td class="mono">{{ alerta.itemId }}</td>
                    <td class="mono">{{ alerta.almoxarifadoId }}</td>
                    <td>{{ alerta.quantidade }}</td>
                    <td>
                      @if (alerta.limite !== null) {
                        Limite: {{ alerta.limite }}
                      }
                      @if (alerta.validade) {
                        Validade: {{ alerta.validade }}
                      }
                    </td>
                    <td>
                      <button
                        type="button"
                        class="btn btn-secondary btn-sm"
                        [disabled]="loading()"
                        (click)="dispensar(alerta)"
                      >
                        Dispensar
                      </button>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </section>
    </div>
  `,
})
export class DashboardPageComponent implements OnInit {
  private readonly service = inject(DashboardService);

  protected readonly loading = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly sucesso = signal<string | null>(null);
  protected readonly dashboard = signal<Record<string, unknown>>({});
  protected readonly alertas = signal<AlertaItem[]>([]);

  protected readonly kpis = computed(() => {
    const entries = Object.entries(this.dashboard());
    return entries.map(([chave, valor]) => ({
      chave,
      label: chave.replace(/([A-Z])/g, ' $1').replace(/^./, (s) => s.toUpperCase()),
      valor: valor === null || valor === undefined ? '-' : String(valor),
    }));
  });

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    this.loading.set(true);
    this.erro.set(null);

    forkJoin({
      dashboard: this.service.dashboard(),
      alertas: this.service.alertas(),
    }).subscribe({
      next: ({ dashboard, alertas }) => {
        this.dashboard.set(dashboard);
        this.alertas.set(alertas);
        this.loading.set(false);
      },
      error: (err) => {
        this.erro.set(extractApiErrorMessage(err, 'Erro ao carregar dashboard e alertas.'));
        this.loading.set(false);
      },
    });
  }

  dispensar(alerta: AlertaItem): void {
    this.loading.set(true);
    this.erro.set(null);
    this.sucesso.set(null);

    this.service.dispensar(alerta.chave, alerta.revisao).subscribe({
      next: () => {
        this.sucesso.set('Alerta dispensado para a revisão atual.');
        this.carregar();
      },
      error: (err) => {
        this.erro.set(extractApiErrorMessage(err, 'Não foi possível dispensar o alerta.'));
        this.loading.set(false);
      },
    });
  }
}
