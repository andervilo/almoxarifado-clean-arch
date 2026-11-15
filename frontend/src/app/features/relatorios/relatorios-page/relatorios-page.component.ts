import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { extractApiErrorMessage } from '../../../core/services/api-error.util';
import { AlertBannerComponent } from '../../../shared/components/alert-banner/alert-banner.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { RelatorioService } from '../relatorio.service';

@Component({
  selector: 'app-relatorios-page',
  standalone: true,
  imports: [ReactiveFormsModule, PageHeaderComponent, AlertBannerComponent, EmptyStateComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-page-header
      title="Relatórios"
      subtitle="Consumo líquido por setor e centro de custo com exportação CSV."
    />

    <div class="stack">
      <app-alert-banner kind="error" [message]="erro()" />

      <section class="card">
        <h2 class="card-title">Consumo por Setor</h2>
        <form class="form-grid" [formGroup]="form" (ngSubmit)="consultar()">
          <div class="field">
            <label for="rel-inicio">Data Início</label>
            <input id="rel-inicio" type="date" formControlName="inicio" />
          </div>
          <div class="field">
            <label for="rel-fim">Data Fim</label>
            <input id="rel-fim" type="date" formControlName="fim" />
          </div>
          <div class="actions-row" style="align-items: flex-end;">
            <button type="submit" class="btn btn-primary" [disabled]="form.invalid || loading()">
              Consultar
            </button>
            <button
              type="button"
              class="btn btn-secondary"
              [disabled]="form.invalid || loading()"
              (click)="exportar()"
            >
              Exportar CSV
            </button>
          </div>
        </form>
      </section>

      <section class="card">
        <h2 class="card-title">Resultados ({{ linhas().length }})</h2>
        @if (linhas().length === 0) {
          <app-empty-state message="Informe o período e clique em Consultar." />
        } @else {
          <div class="table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  @for (col of colunas(); track col) {
                    <th>{{ col }}</th>
                  }
                </tr>
              </thead>
              <tbody>
                @for (row of linhas(); track $index) {
                  <tr>
                    @for (col of colunas(); track col) {
                      <td class="mono">{{ row[col] ?? '—' }}</td>
                    }
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
export class RelatoriosPageComponent {
  private readonly service = inject(RelatorioService);
  private readonly fb = inject(FormBuilder);

  protected readonly loading = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly linhas = signal<Array<Record<string, unknown>>>([]);

  private readonly hoje = new Date().toISOString().slice(0, 10);
  private readonly inicioMes = `${this.hoje.slice(0, 8)}01`;

  protected readonly form = this.fb.nonNullable.group({
    inicio: [this.inicioMes, Validators.required],
    fim: [this.hoje, Validators.required],
  });

  colunas(): string[] {
    const first = this.linhas()[0];
    return first ? Object.keys(first) : [];
  }

  consultar(): void {
    if (this.form.invalid) return;
    const { inicio, fim } = this.form.getRawValue();
    this.loading.set(true);
    this.erro.set(null);

    this.service.consumoPorSetor(inicio, fim).subscribe({
      next: (res) => {
        this.linhas.set(res);
        this.loading.set(false);
      },
      error: (err) => {
        this.erro.set(extractApiErrorMessage(err, 'Erro ao consultar consumo por setor.'));
        this.loading.set(false);
      },
    });
  }

  exportar(): void {
    if (this.form.invalid) return;
    const { inicio, fim } = this.form.getRawValue();
    this.loading.set(true);
    this.erro.set(null);

    this.service.exportarConsumoCsv(inicio, fim).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `consumo-por-setor-${inicio}-a-${fim}.csv`;
        link.click();
        URL.revokeObjectURL(url);
        this.loading.set(false);
      },
      error: (err) => {
        this.erro.set(extractApiErrorMessage(err, 'Erro ao exportar CSV.'));
        this.loading.set(false);
      },
    });
  }
}
