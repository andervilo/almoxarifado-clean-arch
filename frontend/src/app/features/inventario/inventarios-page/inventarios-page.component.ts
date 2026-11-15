import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { extractApiErrorMessage } from '../../../core/services/api-error.util';
import { AlertBannerComponent } from '../../../shared/components/alert-banner/alert-banner.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { ConfiguracaoService, LocalArmazenamento } from '../../configuracoes/configuracao.service';
import { Inventario, InventarioService } from '../inventario.service';

@Component({
  selector: 'app-inventarios-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    PageHeaderComponent,
    AlertBannerComponent,
    EmptyStateComponent,
    StatusBadgeComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-page-header
      title="Inventário Físico"
      subtitle="Abertura de contagem (normal ou cega), registro de contagens, reconciliação e ajuste."
    >
      <button type="button" class="btn btn-secondary" [disabled]="loading()" (click)="carregar()">
        Atualizar
      </button>
    </app-page-header>

    <div class="stack">
      <app-alert-banner kind="error" [message]="erro()" />
      <app-alert-banner kind="success" [message]="sucesso()" />

      @if (podeOperar()) {
        <section class="card">
          <h2 class="card-title">Abrir Novo Inventário</h2>
          <form class="form-grid" [formGroup]="abrirForm" (ngSubmit)="abrir()">
            <div class="field">
              <label for="inv-local">Local</label>
              <select id="inv-local" formControlName="localId">
                <option value="">Selecione...</option>
                @for (loc of locais(); track loc.id) {
                  <option [value]="loc.id">{{ loc.codigo }} — {{ loc.descricao }}</option>
                }
              </select>
            </div>
            <label class="checkbox-field">
              <input type="checkbox" formControlName="contagemCega" />
              Contagem cega (oculta saldo do sistema até finalizar)
            </label>
            <div class="actions-row" style="align-items: flex-end;">
              <button type="submit" class="btn btn-primary" [disabled]="abrirForm.invalid || loading()">
                Abrir Inventário
              </button>
            </div>
          </form>
        </section>
      }

      <section class="card">
        <h2 class="card-title">Inventários ({{ inventarios().length }})</h2>
        @if (inventarios().length === 0) {
          <app-empty-state message="Nenhum inventário aberto." />
        } @else {
          <div class="table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Local</th>
                  <th>Status</th>
                  <th>Modo</th>
                  <th>Posições</th>
                  <th>Ações</th>
                </tr>
              </thead>
              <tbody>
                @for (inv of inventarios(); track inv.id) {
                  <tr>
                    <td class="mono">{{ inv.id }}</td>
                    <td class="mono">{{ nomeLocal(inv.localId) }}</td>
                    <td><app-status-badge [status]="inv.status" /></td>
                    <td>{{ inv.contagemCega ? 'Cega' : 'Normal' }}</td>
                    <td>{{ inv.contagens.length }}</td>
                    <td>
                      <div class="actions-row">
                        <button type="button" class="btn btn-secondary btn-sm" (click)="selecionar(inv.id)">
                          Abrir Contagens
                        </button>
                        @if (inv.status === 'CONTAGEM' && podeOperar()) {
                          <button type="button" class="btn btn-danger btn-sm" (click)="cancelar(inv.id)">
                            Cancelar
                          </button>
                        }
                      </div>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </section>

      @if (selecionado(); as inv) {
        <section class="card">
          <h2 class="card-title">Contagens do Inventário ({{ inv.status }})</h2>
          @if (inv.erro) {
            <app-alert-banner kind="error" [message]="inv.erro" />
          }

          <div class="table-wrap" style="margin-top: 0.75rem;">
            <table class="data-table">
              <thead>
                <tr>
                  <th>Posição</th>
                  <th>Item</th>
                  <th>Lote / Série</th>
                  <th>Sistema</th>
                  <th>Contada</th>
                  <th>Diferença</th>
                  @if (inv.status === 'CONTAGEM' && podeOperar()) {
                    <th>Registrar Contagem</th>
                  }
                </tr>
              </thead>
              <tbody>
                @for (c of inv.contagens; track c.posicaoId) {
                  <tr>
                    <td class="mono">{{ c.posicaoId }}</td>
                    <td class="mono">{{ c.itemId }}</td>
                    <td>{{ c.lote || '—' }} / {{ c.serie || '—' }}</td>
                    <td>{{ c.sistema ?? 'Oculto' }}</td>
                    <td>{{ c.contada ?? 'Pendente' }}</td>
                    <td>{{ c.diferenca ?? '—' }}</td>
                    @if (inv.status === 'CONTAGEM' && podeOperar()) {
                      <td>
                        <div class="actions-row">
                          <input
                            #qtdInput
                            type="number"
                            step="0.0001"
                            min="0"
                            [value]="c.contada ?? 0"
                            style="width: 100px; padding: 0.3rem;"
                          />
                          <button
                            type="button"
                            class="btn btn-primary btn-sm"
                            (click)="registrarContagem(inv.id, c.posicaoId, qtdInput.value)"
                          >
                            Salvar
                          </button>
                        </div>
                      </td>
                    }
                  </tr>
                }
              </tbody>
            </table>
          </div>

          @if (inv.status === 'CONTAGEM' && podeOperar()) {
            <form
              class="form-grid"
              style="margin-top: 1.25rem;"
              [formGroup]="finalizarForm"
              (ngSubmit)="finalizar(inv.id)"
            >
              <div class="field" style="grid-column: span 2;">
                <label for="inv-just">Justificativa para finalização / ajuste</label>
                <input id="inv-just" formControlName="justificativa" placeholder="Contagem cíclica mensal" />
              </div>
              <div class="actions-row" style="align-items: flex-end;">
                <button type="submit" class="btn btn-primary" [disabled]="finalizarForm.invalid || loading()">
                  Finalizar Inventário
                </button>
              </div>
            </form>
          }
        </section>
      }
    </div>
  `,
})
export class InventariosPageComponent implements OnInit {
  private readonly service = inject(InventarioService);
  private readonly configService = inject(ConfiguracaoService);
  private readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder);

  protected readonly loading = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly sucesso = signal<string | null>(null);
  protected readonly inventarios = signal<Inventario[]>([]);
  protected readonly selecionado = signal<Inventario | null>(null);
  protected readonly locais = signal<LocalArmazenamento[]>([]);

  protected readonly abrirForm = this.fb.nonNullable.group({
    localId: ['', Validators.required],
    contagemCega: [false],
  });

  protected readonly finalizarForm = this.fb.nonNullable.group({
    justificativa: ['', Validators.required],
  });

  ngOnInit(): void {
    this.carregar();
  }

  podeOperar(): boolean {
    return this.auth.hasAnyRole(['ADMIN_PLATAFORMA', 'ADMIN_ORGANIZACAO', 'ALMOXARIFE']);
  }

  nomeLocal(localId: string): string {
    const loc = this.locais().find((l) => l.id === localId);
    return loc ? `${loc.codigo} — ${loc.descricao}` : localId;
  }

  carregar(): void {
    this.loading.set(true);
    this.erro.set(null);

    forkJoin({
      inventarios: this.service.listar(),
      almox: this.configService.listarAlmoxarifados(),
    }).subscribe({
      next: ({ inventarios, almox }) => {
        this.inventarios.set(inventarios);
        if (almox.length > 0) {
          forkJoin(almox.map((a) => this.configService.listarLocais(a.id))).subscribe({
            next: (listas) => {
              this.locais.set(listas.flat());
              this.loading.set(false);
            },
            error: () => this.loading.set(false),
          });
        } else {
          this.loading.set(false);
        }
      },
      error: (err) => {
        this.erro.set(extractApiErrorMessage(err, 'Erro ao carregar inventários.'));
        this.loading.set(false);
      },
    });
  }

  abrir(): void {
    if (this.abrirForm.invalid) return;
    this.loading.set(true);
    this.erro.set(null);
    this.sucesso.set(null);

    const { localId, contagemCega } = this.abrirForm.getRawValue();
    this.service.criar(localId, contagemCega).subscribe({
      next: (inv) => {
        this.sucesso.set(`Inventário aberto (${inv.id}).`);
        this.selecionado.set(inv);
        this.carregar();
      },
      error: (err) => {
        this.erro.set(extractApiErrorMessage(err, 'Erro ao abrir inventário.'));
        this.loading.set(false);
      },
    });
  }

  selecionar(id: string): void {
    this.service.buscar(id).subscribe({
      next: (inv) => this.selecionado.set(inv),
      error: (err) => this.erro.set(extractApiErrorMessage(err, 'Erro ao buscar inventário.')),
    });
  }

  registrarContagem(inventarioId: string, posicaoId: string, quantidadeRaw: string): void {
    const quantidade = Number(quantidadeRaw);
    if (Number.isNaN(quantidade) || quantidade < 0) return;

    this.service.contar(inventarioId, posicaoId, quantidade).subscribe({
      next: () => {
        this.sucesso.set('Contagem registrada.');
        this.selecionar(inventarioId);
      },
      error: (err) => this.erro.set(extractApiErrorMessage(err, 'Erro ao registrar contagem.')),
    });
  }

  finalizar(id: string): void {
    if (this.finalizarForm.invalid) return;
    const { justificativa } = this.finalizarForm.getRawValue();

    this.service.finalizar(id, justificativa).subscribe({
      next: () => {
        this.sucesso.set('Finalização solicitada.');
        this.selecionar(id);
        this.carregar();
      },
      error: (err) => this.erro.set(extractApiErrorMessage(err, 'Erro ao finalizar inventário.')),
    });
  }

  cancelar(id: string): void {
    this.service.cancelar(id).subscribe({
      next: () => {
        this.sucesso.set('Inventário cancelado.');
        this.selecionado.set(null);
        this.carregar();
      },
      error: (err) => this.erro.set(extractApiErrorMessage(err, 'Erro ao cancelar inventário.')),
    });
  }
}
