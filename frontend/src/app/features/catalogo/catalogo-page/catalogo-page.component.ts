import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { extractApiErrorMessage } from '../../../core/services/api-error.util';
import { AlertBannerComponent } from '../../../shared/components/alert-banner/alert-banner.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import {
  CadastroAuxiliar,
  CatalogoService,
  ItemCatalogo,
  TipoRastreabilidade,
} from '../catalogo.service';

@Component({
  selector: 'app-catalogo-page',
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
      title="Catálogo de Materiais"
      subtitle="Itens, categorias, unidades de medida, fornecedores e rastreabilidade."
    >
      <button type="button" class="btn btn-secondary" [disabled]="loading()" (click)="carregar()">
        Atualizar
      </button>
    </app-page-header>

    <div class="stack">
      <app-alert-banner kind="error" [message]="erro()" />
      <app-alert-banner kind="success" [message]="sucesso()" />

      <div class="tabs">
        <button type="button" class="tab-btn" [class.active]="aba() === 'itens'" (click)="aba.set('itens')">
          Itens ({{ itens().length }})
        </button>
        <button type="button" class="tab-btn" [class.active]="aba() === 'categorias'" (click)="aba.set('categorias')">
          Categorias ({{ categorias().length }})
        </button>
        <button type="button" class="tab-btn" [class.active]="aba() === 'unidades'" (click)="aba.set('unidades')">
          Unidades ({{ unidades().length }})
        </button>
        <button type="button" class="tab-btn" [class.active]="aba() === 'fornecedores'" (click)="aba.set('fornecedores')">
          Fornecedores ({{ fornecedores().length }})
        </button>
      </div>

      @if (aba() === 'itens') {
        @if (podeEditar()) {
          <section class="card">
            <h2 class="card-title">Novo Item</h2>
            <form class="form-grid" [formGroup]="itemForm" (ngSubmit)="criarItem()">
              <div class="field">
                <label for="item-codigo">Código</label>
                <input id="item-codigo" formControlName="codigo" placeholder="MAT-001" />
              </div>
              <div class="field">
                <label for="item-desc">Descrição</label>
                <input id="item-desc" formControlName="descricao" placeholder="Parafuso M8 x 30" />
              </div>
              <div class="field">
                <label for="item-cat">Categoria</label>
                <select id="item-cat" formControlName="categoriaId">
                  <option value="">Selecione...</option>
                  @for (c of categorias(); track c.id) {
                    <option [value]="c.id">{{ c.codigo }} — {{ c.nome }}</option>
                  }
                </select>
              </div>
              <div class="field">
                <label for="item-un">Unidade de Medida</label>
                <select id="item-un" formControlName="unidadeMedidaId">
                  <option value="">Selecione...</option>
                  @for (u of unidades(); track u.id) {
                    <option [value]="u.id">{{ u.codigo }} — {{ u.nome }}</option>
                  }
                </select>
              </div>
              <div class="field">
                <label for="item-rast">Rastreabilidade</label>
                <select id="item-rast" formControlName="rastreabilidade">
                  <option value="NENHUM">NENHUM</option>
                  <option value="LOTE">LOTE</option>
                  <option value="LOTE_VALIDADE">LOTE_VALIDADE</option>
                  <option value="NUMERO_SERIE">NUMERO_SERIE</option>
                </select>
              </div>
              <div class="field">
                <label for="item-min">Estoque Mínimo</label>
                <input id="item-min" type="number" step="0.0001" formControlName="estoqueMinimo" />
              </div>
              <div class="field">
                <label for="item-max">Estoque Máximo</label>
                <input id="item-max" type="number" step="0.0001" formControlName="estoqueMaximo" />
              </div>
              <div class="actions-row" style="align-items: flex-end;">
                <button type="submit" class="btn btn-primary" [disabled]="itemForm.invalid || loading()">
                  Cadastrar Item
                </button>
              </div>
            </form>
          </section>
        }

        <section class="card">
          <h2 class="card-title">Itens cadastrados</h2>
          @if (itens().length === 0) {
            <app-empty-state message="Nenhum item cadastrado." />
          } @else {
            <div class="table-wrap">
              <table class="data-table">
                <thead>
                  <tr>
                    <th>Código</th>
                    <th>Descrição</th>
                    <th>Rastreabilidade</th>
                    <th>Mín / Máx</th>
                    <th>Status</th>
                    <th>ID</th>
                  </tr>
                </thead>
                <tbody>
                  @for (item of itens(); track item.id) {
                    <tr>
                      <td class="mono">{{ item.codigo }}</td>
                      <td>{{ item.descricao }}</td>
                      <td>{{ item.rastreabilidade }}</td>
                      <td>{{ item.estoqueMinimo }} / {{ item.estoqueMaximo }}</td>
                      <td><app-status-badge [status]="item.ativo ? 'ATIVO' : 'INATIVO'" /></td>
                      <td class="mono">{{ item.id }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          }
        </section>
      } @else {
        @if (podeEditar()) {
          <section class="card">
            <h2 class="card-title">Novo Cadastro ({{ aba() }})</h2>
            <form class="form-grid" [formGroup]="auxForm" (ngSubmit)="criarAuxiliar()">
              <div class="field">
                <label for="aux-codigo">Código</label>
                <input id="aux-codigo" formControlName="codigo" placeholder="COD-01" />
              </div>
              <div class="field">
                <label for="aux-nome">Nome</label>
                <input id="aux-nome" formControlName="nome" placeholder="Nome descritivo" />
              </div>
              <div class="actions-row" style="align-items: flex-end;">
                <button type="submit" class="btn btn-primary" [disabled]="auxForm.invalid || loading()">
                  Salvar
                </button>
              </div>
            </form>
          </section>
        }

        <section class="card">
          <h2 class="card-title">Registros</h2>
          <div class="table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  <th>Código</th>
                  <th>Nome</th>
                  <th>Status</th>
                  <th>ID</th>
                </tr>
              </thead>
              <tbody>
                @for (reg of listaAuxiliarAtual(); track reg.id) {
                  <tr>
                    <td class="mono">{{ reg.codigo }}</td>
                    <td>{{ reg.nome }}</td>
                    <td><app-status-badge [status]="reg.ativo ? 'ATIVO' : 'INATIVO'" /></td>
                    <td class="mono">{{ reg.id }}</td>
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
export class CatalogoPageComponent implements OnInit {
  private readonly service = inject(CatalogoService);
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);

  protected readonly aba = signal<'itens' | 'categorias' | 'unidades' | 'fornecedores'>('itens');
  protected readonly loading = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly sucesso = signal<string | null>(null);

  protected readonly itens = signal<ItemCatalogo[]>([]);
  protected readonly categorias = signal<CadastroAuxiliar[]>([]);
  protected readonly unidades = signal<CadastroAuxiliar[]>([]);
  protected readonly fornecedores = signal<CadastroAuxiliar[]>([]);

  protected readonly itemForm = this.fb.nonNullable.group({
    codigo: ['', Validators.required],
    descricao: ['', Validators.required],
    categoriaId: ['', Validators.required],
    unidadeMedidaId: ['', Validators.required],
    rastreabilidade: ['NENHUM' as TipoRastreabilidade, Validators.required],
    estoqueMinimo: [0, [Validators.required, Validators.min(0)]],
    estoqueMaximo: [0, [Validators.required, Validators.min(0)]],
  });

  protected readonly auxForm = this.fb.nonNullable.group({
    codigo: ['', Validators.required],
    nome: ['', Validators.required],
  });

  ngOnInit(): void {
    this.carregar();
  }

  podeEditar(): boolean {
    return this.auth.hasAnyRole(['ADMIN_PLATAFORMA', 'ADMIN_ORGANIZACAO', 'ALMOXARIFE']);
  }

  listaAuxiliarAtual(): CadastroAuxiliar[] {
    switch (this.aba()) {
      case 'categorias':
        return this.categorias();
      case 'unidades':
        return this.unidades();
      case 'fornecedores':
        return this.fornecedores();
      default:
        return [];
    }
  }

  carregar(): void {
    this.loading.set(true);
    this.erro.set(null);

    forkJoin({
      itens: this.service.listarItens(),
      categorias: this.service.listarCategorias(),
      unidades: this.service.listarUnidades(),
      fornecedores: this.service.listarFornecedores(),
    }).subscribe({
      next: (res) => {
        this.itens.set(res.itens);
        this.categorias.set(res.categorias);
        this.unidades.set(res.unidades);
        this.fornecedores.set(res.fornecedores);
        this.loading.set(false);
      },
      error: (err) => {
        this.erro.set(extractApiErrorMessage(err, 'Erro ao carregar catálogo.'));
        this.loading.set(false);
      },
    });
  }

  criarItem(): void {
    if (this.itemForm.invalid) return;
    this.loading.set(true);
    this.erro.set(null);
    this.sucesso.set(null);

    const v = this.itemForm.getRawValue();
    this.service.criarItem({ ...v, ativo: true }).subscribe({
      next: () => {
        this.sucesso.set('Item cadastrado com sucesso.');
        this.itemForm.reset({
          codigo: '',
          descricao: '',
          categoriaId: v.categoriaId,
          unidadeMedidaId: v.unidadeMedidaId,
          rastreabilidade: 'NENHUM',
          estoqueMinimo: 0,
          estoqueMaximo: 0,
        });
        this.carregar();
      },
      error: (err) => {
        this.erro.set(extractApiErrorMessage(err, 'Erro ao cadastrar item.'));
        this.loading.set(false);
      },
    });
  }

  criarAuxiliar(): void {
    if (this.auxForm.invalid) return;
    this.loading.set(true);
    this.erro.set(null);
    this.sucesso.set(null);

    const payload = { ...this.auxForm.getRawValue(), ativo: true };
    const req$ =
      this.aba() === 'categorias'
        ? this.service.criarCategoria(payload)
        : this.aba() === 'unidades'
          ? this.service.criarUnidade(payload)
          : this.service.criarFornecedor(payload);

    req$.subscribe({
      next: () => {
        this.sucesso.set('Cadastro salvo com sucesso.');
        this.auxForm.reset({ codigo: '', nome: '' });
        this.carregar();
      },
      error: (err) => {
        this.erro.set(extractApiErrorMessage(err, 'Erro ao salvar cadastro.'));
        this.loading.set(false);
      },
    });
  }
}
