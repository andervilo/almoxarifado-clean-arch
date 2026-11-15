import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { extractApiErrorMessage } from '../../../core/services/api-error.util';
import { AlertBannerComponent } from '../../../shared/components/alert-banner/alert-banner.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { CatalogoService, ItemCatalogo } from '../../catalogo/catalogo.service';
import {
  ConfiguracaoService,
  LocalArmazenamento,
  Setor,
} from '../../configuracoes/configuracao.service';
import { AlocacaoReserva, Requisicao, RequisicaoService } from '../requisicao.service';

@Component({
  selector: 'app-requisicoes-page',
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
      title="Requisições de Material"
      subtitle="Criação de pedidos, reserva atômica, separação, entrega integral/parcial e confirmação."
    >
      <button type="button" class="btn btn-secondary" [disabled]="loading()" (click)="carregar()">
        Atualizar
      </button>
    </app-page-header>

    <div class="stack">
      <app-alert-banner kind="error" [message]="erro()" />
      <app-alert-banner kind="success" [message]="sucesso()" />

      @if (podeCriar()) {
        <section class="card">
          <h2 class="card-title">Nova Requisição (Rascunho)</h2>
          <form class="form-grid" [formGroup]="form" (ngSubmit)="criar()">
            <div class="field">
              <label for="req-local">Local de Armazenamento</label>
              <select id="req-local" formControlName="localId">
                <option value="">Selecione...</option>
                @for (loc of locais(); track loc.id) {
                  <option [value]="loc.id">{{ loc.codigo }} — {{ loc.descricao }}</option>
                }
              </select>
            </div>
            <div class="field">
              <label for="req-setor">Setor (opcional)</label>
              <select id="req-setor" formControlName="setorCodigo">
                <option value="">—</option>
                @for (s of setores(); track s.codigo) {
                  <option [value]="s.codigo">{{ s.codigo }} — {{ s.nome }}</option>
                }
              </select>
            </div>
            <div class="field">
              <label for="req-item">Item</label>
              <select id="req-item" formControlName="itemId">
                <option value="">Selecione...</option>
                @for (item of itens(); track item.id) {
                  <option [value]="item.id">{{ item.codigo }} — {{ item.descricao }}</option>
                }
              </select>
            </div>
            <div class="field">
              <label for="req-qtd">Quantidade</label>
              <input id="req-qtd" type="number" step="0.0001" formControlName="quantidade" />
            </div>
            <div class="actions-row" style="align-items: flex-end;">
              <button type="submit" class="btn btn-primary" [disabled]="form.invalid || loading()">
                Criar Rascunho
              </button>
            </div>
          </form>
        </section>
      }

      <section class="card">
        <h2 class="card-title">Requisições ({{ requisicoes().length }})</h2>
        @if (requisicoes().length === 0) {
          <app-empty-state message="Nenhuma requisição registrada." />
        } @else {
          <div class="table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Status</th>
                  <th>Setor / Centro</th>
                  <th>Linhas</th>
                  <th>Erro</th>
                  <th>Ações</th>
                </tr>
              </thead>
              <tbody>
                @for (req of requisicoes(); track req.id) {
                  <tr>
                    <td class="mono">{{ req.id }}</td>
                    <td><app-status-badge [status]="req.status" /></td>
                    <td>{{ req.setorCodigo || '—' }} {{ req.centroCusto ? '(' + req.centroCusto + ')' : '' }}</td>
                    <td>
                      @for (l of req.linhas; track l.itemId) {
                        <div class="mono">
                          {{ nomeItem(l.itemId) }}: sol={{ l.quantidade }}
                          @if (l.atendida !== undefined) {
                            | atend={{ l.atendida }} | pend={{ l.pendente }}
                          }
                        </div>
                      }
                    </td>
                    <td class="muted">{{ req.erro || '—' }}</td>
                    <td>
                      <div class="actions-row">
                        @if (req.status === 'RASCUNHO') {
                          <button type="button" class="btn btn-primary btn-sm" (click)="executarAcao(req.id, 'enviar')">
                            Enviar
                          </button>
                          <button type="button" class="btn btn-danger btn-sm" (click)="executarAcao(req.id, 'cancelar')">
                            Cancelar
                          </button>
                        }
                        @if (req.status === 'RESERVADA' && podeAtender()) {
                          <button type="button" class="btn btn-primary btn-sm" (click)="executarAcao(req.id, 'separar')">
                            Separar
                          </button>
                          <button type="button" class="btn btn-secondary btn-sm" (click)="verAlocacoes(req.id)">
                            Alocações
                          </button>
                          <button type="button" class="btn btn-danger btn-sm" (click)="executarAcao(req.id, 'cancelar')">
                            Cancelar
                          </button>
                        }
                        @if ((req.status === 'SEPARADA' || req.status === 'PARCIALMENTE_ENTREGUE') && podeAtender()) {
                          <button type="button" class="btn btn-primary btn-sm" (click)="executarAcao(req.id, 'entregar')">
                            Entregar Restante
                          </button>
                          <button type="button" class="btn btn-secondary btn-sm" (click)="verAlocacoes(req.id)">
                            Alocações
                          </button>
                          <button type="button" class="btn btn-danger btn-sm" (click)="executarAcao(req.id, 'cancelar')">
                            Cancelar
                          </button>
                        }
                        @if (req.status === 'ENTREGUE') {
                          <button type="button" class="btn btn-primary btn-sm" (click)="executarAcao(req.id, 'confirmar-recebimento')">
                            Confirmar Recebimento
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

      @if (alocacoesSelecionadas().length > 0) {
        <section class="card">
          <h2 class="card-title">Alocações da Requisição</h2>
          <div class="table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  <th>Item</th>
                  <th>Posição</th>
                  <th>Lote</th>
                  <th>Série</th>
                  <th>Validade</th>
                  <th>Quantidade</th>
                </tr>
              </thead>
              <tbody>
                @for (a of alocacoesSelecionadas(); track a.posicaoId + a.itemId) {
                  <tr>
                    <td>{{ nomeItem(a.itemId) }}</td>
                    <td class="mono">{{ a.posicaoId }}</td>
                    <td>{{ a.lote || '—' }}</td>
                    <td>{{ a.serie || '—' }}</td>
                    <td>{{ a.validade || '—' }}</td>
                    <td>{{ a.quantidade }}</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        </section>
      }
    </div>
  `,
})
export class RequisicoesPageComponent implements OnInit {
  private readonly service = inject(RequisicaoService);
  private readonly catalogoService = inject(CatalogoService);
  private readonly configService = inject(ConfiguracaoService);
  private readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder);

  protected readonly loading = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly sucesso = signal<string | null>(null);

  protected readonly requisicoes = signal<Requisicao[]>([]);
  protected readonly itens = signal<ItemCatalogo[]>([]);
  protected readonly locais = signal<LocalArmazenamento[]>([]);
  protected readonly setores = signal<Setor[]>([]);
  protected readonly alocacoesSelecionadas = signal<AlocacaoReserva[]>([]);

  protected readonly form = this.fb.nonNullable.group({
    localId: ['', Validators.required],
    setorCodigo: [''],
    itemId: ['', Validators.required],
    quantidade: [1, [Validators.required, Validators.min(0.0001)]],
  });

  ngOnInit(): void {
    this.carregar();
  }

  podeCriar(): boolean {
    return this.auth.hasAnyRole(['ADMIN_PLATAFORMA', 'ADMIN_ORGANIZACAO', 'ALMOXARIFE', 'REQUISITANTE']);
  }

  podeAtender(): boolean {
    return this.auth.hasAnyRole(['ADMIN_PLATAFORMA', 'ADMIN_ORGANIZACAO', 'ALMOXARIFE']);
  }

  nomeItem(itemId: string): string {
    const item = this.itens().find((i) => i.id === itemId);
    return item ? `${item.codigo}` : itemId;
  }

  carregar(): void {
    this.loading.set(true);
    this.erro.set(null);

    forkJoin({
      requisicoes: this.service.listar(),
      itens: this.catalogoService.listarItens(),
      setores: this.configService.listarSetores(),
      almox: this.configService.listarAlmoxarifados(),
    }).subscribe({
      next: ({ requisicoes, itens, setores, almox }) => {
        this.requisicoes.set(requisicoes);
        this.itens.set(itens);
        this.setores.set(setores);
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
        this.erro.set(extractApiErrorMessage(err, 'Erro ao carregar requisições.'));
        this.loading.set(false);
      },
    });
  }

  criar(): void {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.erro.set(null);
    this.sucesso.set(null);

    const v = this.form.getRawValue();
    this.service
      .criar(v.localId, [{ itemId: v.itemId, quantidade: v.quantidade }], v.setorCodigo)
      .subscribe({
        next: (r) => {
          this.sucesso.set(`Requisição criada (${r.id}).`);
          this.carregar();
        },
        error: (err) => {
          this.erro.set(extractApiErrorMessage(err, 'Erro ao criar requisição.'));
          this.loading.set(false);
        },
      });
  }

  executarAcao(
    id: string,
    acao: 'enviar' | 'separar' | 'entregar' | 'cancelar' | 'confirmar-recebimento',
  ): void {
    this.loading.set(true);
    this.erro.set(null);
    this.sucesso.set(null);

    this.service.acao(id, acao).subscribe({
      next: (r) => {
        this.sucesso.set(`Ação '${acao}' aceita (status: ${r.status}).`);
        this.carregar();
      },
      error: (err) => {
        this.erro.set(extractApiErrorMessage(err, `Erro ao executar '${acao}'.`));
        this.loading.set(false);
      },
    });
  }

  verAlocacoes(id: string): void {
    this.service.alocacoes(id).subscribe({
      next: (res) => this.alocacoesSelecionadas.set(res),
      error: (err) => this.erro.set(extractApiErrorMessage(err, 'Erro ao carregar alocações.')),
    });
  }
}
