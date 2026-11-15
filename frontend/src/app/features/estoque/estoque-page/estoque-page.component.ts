import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { extractApiErrorMessage } from '../../../core/services/api-error.util';
import { AlertBannerComponent } from '../../../shared/components/alert-banner/alert-banner.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { CatalogoService, ItemCatalogo } from '../../catalogo/catalogo.service';
import {
  Almoxarifado,
  ConfiguracaoService,
  LocalArmazenamento,
} from '../../configuracoes/configuracao.service';
import { EstoqueService, SaldoPosicao, TipoOperacaoEstoque } from '../estoque.service';

@Component({
  selector: 'app-estoque-page',
  standalone: true,
  imports: [ReactiveFormsModule, PageHeaderComponent, AlertBannerComponent, EmptyStateComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-page-header
      title="Estoque & Kardex"
      subtitle="Saldos por posição/lote/série, custo médio, movimentações idempotentes e histórico."
    />

    <div class="stack">
      <app-alert-banner kind="error" [message]="erro()" />
      <app-alert-banner kind="success" [message]="sucesso()" />

      <section class="card">
        <h2 class="card-title">Consultar Item</h2>
        <div class="form-grid">
          <div class="field">
            <label for="est-item-filtro">Item do catálogo</label>
            <select
              id="est-item-filtro"
              [value]="itemSelecionado()"
              (change)="selecionarItem($any($event.target).value)"
            >
              <option value="">Selecione um item...</option>
              @for (item of itens(); track item.id) {
                <option [value]="item.id">
                  {{ item.codigo }} — {{ item.descricao }} ({{ item.rastreabilidade }})
                </option>
              }
            </select>
          </div>
          <div class="field">
            <label for="est-lote-filtro">Filtrar Kardex por Lote</label>
            <input
              id="est-lote-filtro"
              [value]="filtroLote()"
              (input)="filtroLote.set($any($event.target).value)"
              placeholder="Opcional"
            />
          </div>
          <div class="field">
            <label for="est-serie-filtro">Filtrar Kardex por Série</label>
            <input
              id="est-serie-filtro"
              [value]="filtroSerie()"
              (input)="filtroSerie.set($any($event.target).value)"
              placeholder="Opcional"
            />
          </div>
          <div class="actions-row" style="align-items: flex-end;">
            <button
              type="button"
              class="btn btn-secondary"
              [disabled]="!itemSelecionado() || loading()"
              (click)="consultarItem()"
            >
              Consultar
            </button>
          </div>
        </div>
      </section>

      @if (podeMovimentar()) {
        <section class="card">
          <h2 class="card-title">Nova Movimentação</h2>
          <form class="form-grid" [formGroup]="movForm" (ngSubmit)="movimentar()">
            <div class="field">
              <label for="mov-tipo">Operação</label>
              <select id="mov-tipo" formControlName="tipo">
                <option value="entradas">Entrada</option>
                <option value="saidas">Saída</option>
                <option value="transferencias">Transferência</option>
                <option value="ajustes">Ajuste</option>
                <option value="estornos">Estorno</option>
              </select>
            </div>
            <div class="field">
              <label for="mov-item">Item</label>
              <select id="mov-item" formControlName="itemId">
                <option value="">Selecione...</option>
                @for (item of itens(); track item.id) {
                  <option [value]="item.id">{{ item.codigo }} — {{ item.descricao }}</option>
                }
              </select>
            </div>
            <div class="field">
              <label for="mov-origem">Local Origem (saída/transf.)</label>
              <select id="mov-origem" formControlName="origemId">
                <option value="">—</option>
                @for (loc of locais(); track loc.id) {
                  <option [value]="loc.id">{{ loc.codigo }} — {{ loc.descricao }}</option>
                }
              </select>
            </div>
            <div class="field">
              <label for="mov-destino">Local Destino (entrada/transf./ajuste)</label>
              <select id="mov-destino" formControlName="destinoId">
                <option value="">—</option>
                @for (loc of locais(); track loc.id) {
                  <option [value]="loc.id">{{ loc.codigo }} — {{ loc.descricao }}</option>
                }
              </select>
            </div>
            <div class="field">
              <label for="mov-qtd">Quantidade</label>
              <input id="mov-qtd" type="number" step="0.0001" formControlName="quantidade" />
            </div>
            <div class="field">
              <label for="mov-custo">Custo unitário</label>
              <input id="mov-custo" type="number" step="0.000001" formControlName="custo" />
            </div>
            <div class="field">
              <label for="mov-lote">Lote</label>
              <input id="mov-lote" formControlName="lote" placeholder="Opcional" />
            </div>
            <div class="field">
              <label for="mov-serie">Série</label>
              <input id="mov-serie" formControlName="serie" placeholder="Opcional" />
            </div>
            <div class="field">
              <label for="mov-val">Validade</label>
              <input id="mov-val" type="date" formControlName="validade" />
            </div>
            <div class="field">
              <label for="mov-ref">ID Operação Ref. (estorno)</label>
              <input id="mov-ref" formControlName="referenciaId" placeholder="UUID para estorno" />
            </div>
            <div class="field" style="grid-column: span 2;">
              <label for="mov-motivo">Motivo</label>
              <input id="mov-motivo" formControlName="motivo" placeholder="Recebimento NF 123 / Consumo" />
            </div>
            <div class="actions-row" style="align-items: flex-end;">
              <button type="submit" class="btn btn-primary" [disabled]="movForm.invalid || loading()">
                Executar Movimentação
              </button>
            </div>
          </form>
        </section>
      }

      <section class="card">
        <h2 class="card-title">Saldos por Posição ({{ saldos().length }})</h2>
        @if (saldos().length === 0) {
          <app-empty-state message="Selecione um item para visualizar saldos." />
        } @else {
          <div class="table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  <th>Local</th>
                  <th>Lote</th>
                  <th>Série</th>
                  <th>Validade</th>
                  <th>Físico</th>
                  <th>Reservado</th>
                  <th>Disponível</th>
                  <th>Custo</th>
                </tr>
              </thead>
              <tbody>
                @for (s of saldos(); track s.id) {
                  <tr>
                    <td class="mono">{{ nomeLocal(s.localId) }}</td>
                    <td>{{ s.lote || '—' }}</td>
                    <td>{{ s.serie || '—' }}</td>
                    <td>{{ s.validade || '—' }}</td>
                    <td>{{ s.quantidade }}</td>
                    <td>{{ s.reservado }}</td>
                    <td><strong>{{ s.disponivel }}</strong></td>
                    <td>{{ s.custo ?? '—' }}</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </section>

      <section class="card">
        <h2 class="card-title">Kardex ({{ kardex().length }})</h2>
        @if (kardex().length === 0) {
          <app-empty-state message="Nenhum lançamento de kardex carregado." />
        } @else {
          <div class="table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  @for (col of kardexColunas(); track col) {
                    <th>{{ col }}</th>
                  }
                </tr>
              </thead>
              <tbody>
                @for (row of kardex(); track $index) {
                  <tr>
                    @for (col of kardexColunas(); track col) {
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
export class EstoquePageComponent implements OnInit {
  private readonly estoqueService = inject(EstoqueService);
  private readonly catalogoService = inject(CatalogoService);
  private readonly configService = inject(ConfiguracaoService);
  private readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder);

  protected readonly loading = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly sucesso = signal<string | null>(null);

  protected readonly itens = signal<ItemCatalogo[]>([]);
  protected readonly locais = signal<LocalArmazenamento[]>([]);
  protected readonly almoxarifados = signal<Almoxarifado[]>([]);
  protected readonly itemSelecionado = signal('');
  protected readonly filtroLote = signal('');
  protected readonly filtroSerie = signal('');

  protected readonly saldos = signal<SaldoPosicao[]>([]);
  protected readonly kardex = signal<Array<Record<string, unknown>>>([]);

  protected readonly movForm = this.fb.nonNullable.group({
    tipo: ['entradas' as TipoOperacaoEstoque, Validators.required],
    itemId: ['', Validators.required],
    origemId: [''],
    destinoId: [''],
    quantidade: [1],
    custo: [0],
    lote: [''],
    serie: [''],
    validade: [''],
    referenciaId: [''],
    motivo: ['', Validators.required],
  });

  ngOnInit(): void {
    this.carregarCadastros();
  }

  podeMovimentar(): boolean {
    return this.auth.hasAnyRole(['ADMIN_PLATAFORMA', 'ADMIN_ORGANIZACAO', 'ALMOXARIFE']);
  }

  kardexColunas(): string[] {
    const first = this.kardex()[0];
    return first ? Object.keys(first) : [];
  }

  nomeLocal(localId: string): string {
    const loc = this.locais().find((l) => l.id === localId);
    return loc ? `${loc.codigo} — ${loc.descricao}` : localId;
  }

  selecionarItem(itemId: string): void {
    this.itemSelecionado.set(itemId);
    if (itemId) {
      this.movForm.patchValue({ itemId });
      this.consultarItem();
    }
  }

  carregarCadastros(): void {
    this.loading.set(true);
    forkJoin({
      itens: this.catalogoService.listarItens(),
      almox: this.configService.listarAlmoxarifados(),
    }).subscribe({
      next: ({ itens, almox }) => {
        this.itens.set(itens);
        this.almoxarifados.set(almox);
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
        this.erro.set(extractApiErrorMessage(err, 'Erro ao carregar itens e locais.'));
        this.loading.set(false);
      },
    });
  }

  consultarItem(): void {
    const itemId = this.itemSelecionado();
    if (!itemId) return;

    this.loading.set(true);
    this.erro.set(null);

    forkJoin({
      saldos: this.estoqueService.saldos(itemId),
      kardex: this.estoqueService.kardex(
        itemId,
        this.filtroLote().trim() || undefined,
        this.filtroSerie().trim() || undefined,
      ),
    }).subscribe({
      next: ({ saldos, kardex }) => {
        this.saldos.set(saldos);
        this.kardex.set(kardex);
        this.loading.set(false);
      },
      error: (err) => {
        this.erro.set(extractApiErrorMessage(err, 'Erro ao consultar saldos e kardex.'));
        this.loading.set(false);
      },
    });
  }

  movimentar(): void {
    if (this.movForm.invalid) return;
    this.loading.set(true);
    this.erro.set(null);
    this.sucesso.set(null);

    const v = this.movForm.getRawValue();
    this.estoqueService
      .movimentar(v.tipo, {
        itemId: v.itemId,
        origemId: v.origemId || null,
        destinoId: v.destinoId || null,
        quantidade: v.quantidade,
        lote: v.lote || null,
        serie: v.serie || null,
        validade: v.validade || null,
        custo: v.custo,
        motivo: v.motivo,
        referenciaId: v.referenciaId || null,
      })
      .subscribe({
        next: (res) => {
          this.sucesso.set(`Movimentação registrada (ID: ${res.id}).`);
          this.itemSelecionado.set(v.itemId);
          this.consultarItem();
        },
        error: (err) => {
          this.erro.set(extractApiErrorMessage(err, 'Erro ao registrar movimentação.'));
          this.loading.set(false);
        },
      });
  }
}
