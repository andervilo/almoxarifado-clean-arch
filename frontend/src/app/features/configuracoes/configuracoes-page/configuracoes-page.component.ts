import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { PerfilUsuario } from '../../../core/models/auth.model';
import { extractApiErrorMessage } from '../../../core/services/api-error.util';
import { AlertBannerComponent } from '../../../shared/components/alert-banner/alert-banner.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import {
  Almoxarifado,
  ConfiguracaoService,
  LocalArmazenamento,
  Setor,
  UsuarioLocal,
} from '../configuracao.service';

@Component({
  selector: 'app-configuracoes-page',
  standalone: true,
  imports: [ReactiveFormsModule, PageHeaderComponent, AlertBannerComponent, StatusBadgeComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-page-header
      title="Configurações & Estrutura"
      subtitle="Organização, almoxarifados, locais de armazenamento, setores e provisionamento de usuários."
    >
      <button type="button" class="btn btn-secondary" [disabled]="loading()" (click)="carregar()">
        Atualizar
      </button>
    </app-page-header>

    <div class="stack">
      <app-alert-banner kind="error" [message]="erro()" />
      <app-alert-banner kind="success" [message]="sucesso()" />

      <div class="tabs">
        <button type="button" class="tab-btn" [class.active]="aba() === 'estrutura'" (click)="aba.set('estrutura')">
          Almoxarifados & Locais
        </button>
        <button type="button" class="tab-btn" [class.active]="aba() === 'setores'" (click)="aba.set('setores')">
          Setores ({{ setores().length }})
        </button>
        <button type="button" class="tab-btn" [class.active]="aba() === 'organizacao'" (click)="aba.set('organizacao')">
          Organização & Alertas
        </button>
        @if (ehAdmin()) {
          <button type="button" class="tab-btn" [class.active]="aba() === 'usuarios'" (click)="aba.set('usuarios')">
            Usuários ({{ usuarios().length }})
          </button>
        }
      </div>

      @if (aba() === 'estrutura') {
        @if (ehAdmin()) {
          <section class="card">
            <h2 class="card-title">Novo Almoxarifado</h2>
            <form class="form-grid" [formGroup]="almoxForm" (ngSubmit)="criarAlmoxarifado()">
              <div class="field">
                <label for="alm-cod">Código</label>
                <input id="alm-cod" formControlName="codigo" placeholder="ALM-01" />
              </div>
              <div class="field">
                <label for="alm-nome">Nome</label>
                <input id="alm-nome" formControlName="nome" placeholder="Almoxarifado Central" />
              </div>
              <div class="actions-row" style="align-items: flex-end;">
                <button type="submit" class="btn btn-primary" [disabled]="almoxForm.invalid || loading()">
                  Criar Almoxarifado
                </button>
              </div>
            </form>
          </section>
        }

        <section class="card">
          <h2 class="card-title">Almoxarifados ({{ almoxarifados().length }})</h2>
          <div class="table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  <th>Código</th>
                  <th>Nome</th>
                  <th>Status</th>
                  <th>ID</th>
                  <th>Ações</th>
                </tr>
              </thead>
              <tbody>
                @for (a of almoxarifados(); track a.id) {
                  <tr>
                    <td class="mono">{{ a.codigo }}</td>
                    <td>{{ a.nome }}</td>
                    <td><app-status-badge [status]="a.ativo ? 'ATIVO' : 'INATIVO'" /></td>
                    <td class="mono">{{ a.id }}</td>
                    <td>
                      <button type="button" class="btn btn-secondary btn-sm" (click)="selecionarAlmoxarifado(a.id)">
                        Ver Locais
                      </button>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        </section>

        @if (almoxSelecionado()) {
          <section class="card">
            <h2 class="card-title">Locais do Almoxarifado ({{ locais().length }})</h2>
            @if (ehAdmin() || auth.hasAnyRole(['ALMOXARIFE'])) {
              <form class="form-grid" style="margin-bottom: 1rem;" [formGroup]="localForm" (ngSubmit)="criarLocal()">
                <div class="field">
                  <label for="loc-cod">Código</label>
                  <input id="loc-cod" formControlName="codigo" placeholder="A-01-01" />
                </div>
                <div class="field">
                  <label for="loc-desc">Descrição</label>
                  <input id="loc-desc" formControlName="descricao" placeholder="Corredor A / Prateleira 1" />
                </div>
                <div class="actions-row" style="align-items: flex-end;">
                  <button type="submit" class="btn btn-primary" [disabled]="localForm.invalid || loading()">
                    Criar Local
                  </button>
                </div>
              </form>
            }

            <div class="table-wrap">
              <table class="data-table">
                <thead>
                  <tr>
                    <th>Código</th>
                    <th>Descrição</th>
                    <th>Status</th>
                    <th>ID</th>
                  </tr>
                </thead>
                <tbody>
                  @for (l of locais(); track l.id) {
                    <tr>
                      <td class="mono">{{ l.codigo }}</td>
                      <td>{{ l.descricao }}</td>
                      <td><app-status-badge [status]="l.ativo ? 'ATIVO' : 'INATIVO'" /></td>
                      <td class="mono">{{ l.id }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          </section>
        }
      }

      @if (aba() === 'setores') {
        @if (ehAdmin()) {
          <section class="card">
            <h2 class="card-title">Salvar Setor / Centro de Custo</h2>
            <form class="form-grid" [formGroup]="setorForm" (ngSubmit)="salvarSetor()">
              <div class="field">
                <label for="set-cod">Código</label>
                <input id="set-cod" formControlName="codigo" placeholder="MANUT" />
              </div>
              <div class="field">
                <label for="set-nome">Nome</label>
                <input id="set-nome" formControlName="nome" placeholder="Manutenção Industrial" />
              </div>
              <div class="field">
                <label for="set-cc">Centro de Custo</label>
                <input id="set-cc" formControlName="centroCusto" placeholder="CC-100" />
              </div>
              <div class="actions-row" style="align-items: flex-end;">
                <button type="submit" class="btn btn-primary" [disabled]="setorForm.invalid || loading()">
                  Salvar Setor
                </button>
              </div>
            </form>
          </section>
        }

        <section class="card">
          <h2 class="card-title">Setores</h2>
          <div class="table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  <th>Código</th>
                  <th>Nome</th>
                  <th>Centro de Custo</th>
                </tr>
              </thead>
              <tbody>
                @for (s of setores(); track s.codigo) {
                  <tr>
                    <td class="mono">{{ s.codigo }}</td>
                    <td>{{ s.nome }}</td>
                    <td class="mono">{{ s.centroCusto || '—' }}</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        </section>
      }

      @if (aba() === 'organizacao') {
        <section class="card">
          <h2 class="card-title">Parâmetros da Organização</h2>
          <form class="form-grid" [formGroup]="configForm" (ngSubmit)="salvarConfig()">
            <div class="field">
              <label for="org-dias">Antecedência de alerta de validade (dias: 1 a 365)</label>
              <input id="org-dias" type="number" min="1" max="365" formControlName="diasAlertaValidade" />
            </div>
            @if (ehAdmin()) {
              <div class="actions-row" style="align-items: flex-end;">
                <button type="submit" class="btn btn-primary" [disabled]="configForm.invalid || loading()">
                  Salvar Configuração
                </button>
              </div>
            }
          </form>
        </section>
      }

      @if (aba() === 'usuarios' && ehAdmin()) {
        <section class="card">
          <h2 class="card-title">Provisionar Conta no Keycloak</h2>
          <form class="form-grid" [formGroup]="provForm" (ngSubmit)="provisionar()">
            <div class="field">
              <label for="prov-user">Username</label>
              <input id="prov-user" formControlName="username" placeholder="joao.silva" />
            </div>
            <div class="field">
              <label for="prov-nome">Nome</label>
              <input id="prov-nome" formControlName="nome" placeholder="João Silva" />
            </div>
            <div class="field">
              <label for="prov-email">E-mail</label>
              <input id="prov-email" type="email" formControlName="email" placeholder="joao@empresa.local" />
            </div>
            <div class="field">
              <label for="prov-perfil">Perfil</label>
              <select id="prov-perfil" formControlName="perfil">
                <option value="ALMOXARIFE">ALMOXARIFE</option>
                <option value="REQUISITANTE">REQUISITANTE</option>
                <option value="GESTOR">GESTOR</option>
                <option value="ADMIN_ORGANIZACAO">ADMIN_ORGANIZACAO</option>
              </select>
            </div>
            <div class="actions-row" style="align-items: flex-end;">
              <button type="submit" class="btn btn-primary" [disabled]="provForm.invalid || loading()">
                Provisionar Usuário
              </button>
            </div>
          </form>
        </section>

        <section class="card">
          <h2 class="card-title">Usuários Locais ({{ usuarios().length }})</h2>
          <div class="table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  <th>Código / Identidade</th>
                  <th>Nome</th>
                  <th>E-mail</th>
                  <th>Perfil</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                @for (u of usuarios(); track u.codigo) {
                  <tr>
                    <td class="mono">{{ u.codigo }}</td>
                    <td>{{ u.nome }}</td>
                    <td>{{ u.email }}</td>
                    <td>{{ u.perfil }}</td>
                    <td><app-status-badge [status]="u.ativo ? 'ATIVO' : 'INATIVO'" /></td>
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
export class ConfiguracoesPageComponent implements OnInit {
  protected readonly auth = inject(AuthService);
  private readonly service = inject(ConfiguracaoService);
  private readonly fb = inject(FormBuilder);

  protected readonly aba = signal<'estrutura' | 'setores' | 'organizacao' | 'usuarios'>('estrutura');
  protected readonly loading = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly sucesso = signal<string | null>(null);

  protected readonly almoxarifados = signal<Almoxarifado[]>([]);
  protected readonly almoxSelecionado = signal<string | null>(null);
  protected readonly locais = signal<LocalArmazenamento[]>([]);
  protected readonly setores = signal<Setor[]>([]);
  protected readonly usuarios = signal<UsuarioLocal[]>([]);

  protected readonly almoxForm = this.fb.nonNullable.group({
    codigo: ['', Validators.required],
    nome: ['', Validators.required],
  });

  protected readonly localForm = this.fb.nonNullable.group({
    codigo: ['', Validators.required],
    descricao: ['', Validators.required],
  });

  protected readonly setorForm = this.fb.nonNullable.group({
    codigo: ['', Validators.required],
    nome: ['', Validators.required],
    centroCusto: [''],
  });

  protected readonly configForm = this.fb.nonNullable.group({
    diasAlertaValidade: [30, [Validators.required, Validators.min(1), Validators.max(365)]],
  });

  protected readonly provForm = this.fb.nonNullable.group({
    username: ['', Validators.required],
    nome: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    perfil: ['ALMOXARIFE' as PerfilUsuario, Validators.required],
  });

  ngOnInit(): void {
    this.carregar();
  }

  ehAdmin(): boolean {
    return this.auth.hasAnyRole(['ADMIN_PLATAFORMA', 'ADMIN_ORGANIZACAO']);
  }

  carregar(): void {
    this.loading.set(true);
    this.erro.set(null);

    forkJoin({
      almox: this.service.listarAlmoxarifados(),
      setores: this.service.listarSetores(),
      config: this.service.configuracoes(),
    }).subscribe({
      next: ({ almox, setores, config }) => {
        this.almoxarifados.set(almox);
        this.setores.set(setores);
        this.configForm.patchValue({ diasAlertaValidade: config.diasAlertaValidade });
        if (this.ehAdmin()) {
          this.service.listarUsuarios().subscribe({
            next: (u) => this.usuarios.set(u),
          });
        }
        this.loading.set(false);
      },
      error: (err) => {
        this.erro.set(extractApiErrorMessage(err, 'Erro ao carregar configurações.'));
        this.loading.set(false);
      },
    });
  }

  selecionarAlmoxarifado(id: string): void {
    this.almoxSelecionado.set(id);
    this.service.listarLocais(id).subscribe({
      next: (l) => this.locais.set(l),
      error: (err) => this.erro.set(extractApiErrorMessage(err, 'Erro ao listar locais.')),
    });
  }

  criarAlmoxarifado(): void {
    if (this.almoxForm.invalid) return;
    const { codigo, nome } = this.almoxForm.getRawValue();
    this.service.criarAlmoxarifado(codigo, nome).subscribe({
      next: () => {
        this.sucesso.set('Almoxarifado criado.');
        this.almoxForm.reset({ codigo: '', nome: '' });
        this.carregar();
      },
      error: (err) => this.erro.set(extractApiErrorMessage(err, 'Erro ao criar almoxarifado.')),
    });
  }

  criarLocal(): void {
    const almoxId = this.almoxSelecionado();
    if (!almoxId || this.localForm.invalid) return;
    const { codigo, descricao } = this.localForm.getRawValue();
    this.service.criarLocal(almoxId, codigo, descricao).subscribe({
      next: () => {
        this.sucesso.set('Local criado.');
        this.localForm.reset({ codigo: '', descricao: '' });
        this.selecionarAlmoxarifado(almoxId);
      },
      error: (err) => this.erro.set(extractApiErrorMessage(err, 'Erro ao criar local.')),
    });
  }

  salvarSetor(): void {
    if (this.setorForm.invalid) return;
    const v = this.setorForm.getRawValue();
    this.service
      .salvarSetor({ codigo: v.codigo, nome: v.nome, centroCusto: v.centroCusto || null })
      .subscribe({
        next: () => {
          this.sucesso.set('Setor salvo.');
          this.setorForm.reset({ codigo: '', nome: '', centroCusto: '' });
          this.carregar();
        },
        error: (err) => this.erro.set(extractApiErrorMessage(err, 'Erro ao salvar setor.')),
      });
  }

  salvarConfig(): void {
    if (this.configForm.invalid) return;
    const { diasAlertaValidade } = this.configForm.getRawValue();
    this.service.salvarConfiguracoes(diasAlertaValidade).subscribe({
      next: () => this.sucesso.set('Configuração de validade atualizada.'),
      error: (err) => this.erro.set(extractApiErrorMessage(err, 'Erro ao salvar configuração.')),
    });
  }

  provisionar(): void {
    if (this.provForm.invalid) return;
    const v = this.provForm.getRawValue();
    this.service
      .provisionarUsuario({
        ...v,
        ativo: true,
        almoxarifados: null,
      })
      .subscribe({
        next: (res) => {
          this.sucesso.set(`Provisionamento solicitado (${res.id} — ${res.status}).`);
          this.provForm.reset({ username: '', nome: '', email: '', perfil: 'ALMOXARIFE' });
          this.carregar();
        },
        error: (err) => this.erro.set(extractApiErrorMessage(err, 'Erro ao provisionar usuário.')),
      });
  }
}
